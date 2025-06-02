package com.haemulzzzim.fintobe.meeting_room.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.haemulzzzim.fintobe.meeting_room.entity.Meeting;
import com.haemulzzzim.fintobe.meeting_room.entity.Room;
import com.haemulzzzim.fintobe.meeting_room.repository.MeetingRepository;

/**
 * Redis와 DB 간의 회의실 예약 데이터를 주기적으로 동기화하는 스케줄러
 */
@Component
@EnableScheduling
public class DataSynchronizer {
    private static final Logger logger = LoggerFactory.getLogger(DataSynchronizer.class);

    private final RoomTimeSlotService roomTimeSlotService;
    private final MeetingRepository meetingRepository;

    @Autowired
    public DataSynchronizer(RoomTimeSlotService roomTimeSlotService,
            MeetingRepository meetingRepository) {
        this.roomTimeSlotService = roomTimeSlotService;
        this.meetingRepository = meetingRepository;
    }

    /**
     * 1시간마다 실행되는 Redis-DB 동기화 작업
     * 기존 Redis 데이터는 유지하면서 DB 데이터와 일치시킵니다.
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void synchronizeRedisWithDatabase() {
        logger.info("Redis-DB 동기화 작업 시작: {}", LocalDateTime.now());

        try {
            // 오늘과 미래 날짜의 회의만 동기화
            LocalDate today = LocalDate.now();
            List<Meeting> meetings = meetingRepository.findByDateGreaterThanEqual(today);

            logger.info("DB에서 조회된 미래 회의 수: {}", meetings.size());

            if (meetings.isEmpty()) {
                logger.info("미래 예약된 회의가 없어 동기화 작업을 종료합니다.");
                return;
            }

            // 첫 번째 접근 방식: Redis를 완전히 초기화하지 않고 개별 회의별로 처리
            // DB에 있는 회의 데이터를 기반으로 Redis에 예약 정보 업데이트
            int processedCount = 0;
            int successCount = 0;
            int failCount = 0;

            for (Meeting meeting : meetings) {
                try {
                    Room room = meeting.getRoom();
                    LocalDate date = meeting.getDate();
                    LocalTime startTime = meeting.getStartTime();
                    LocalTime endTime = meeting.getEndTime();

                    // 1. 기존 Redis에 예약이 없는 경우: 새로 예약
                    // 2. 기존 Redis에 예약이 있지만 정보가 다른 경우: 갱신
                    // 3. 기존 Redis에 예약이 있고 정보가 같은 경우: 유지
                    boolean reserved = roomTimeSlotService.reserveTimeRange(room.getRoomId(), date, startTime, endTime);

                    processedCount++;
                    if (reserved) {
                        successCount++;
                        logger.debug("회의실 예약 동기화: 회의ID={}, 회의실={}, 날짜={}, 시간={}-{}",
                                meeting.getMeetingId(), room.getName(), date, startTime, endTime);
                    } else {
                        failCount++;
                        logger.warn("회의실 예약 동기화 실패: 회의ID={}, 회의실={}, 날짜={}, 시간={}-{}",
                                meeting.getMeetingId(), room.getName(), date, startTime, endTime);
                    }
                } catch (Exception e) {
                    failCount++;
                    logger.error("회의 동기화 중 개별 처리 오류 (회의ID={}): {}", meeting.getMeetingId(), e.getMessage());
                }
            }

            logger.info("Redis-DB 동기화 작업 완료: 총 {}개 회의 중 {}개 성공, {}개 실패, {}",
                    processedCount, successCount, failCount, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Redis-DB 동기화 작업 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * Redis와 DB 간의 데이터를 완전히 재동기화 (관리자용)
     * 이 메서드는 스케줄로 실행되지 않고, 필요할 때 수동으로 호출해야 합니다.
     */
    public void fullResynchronization() {
        logger.info("Redis-DB 전체 재동기화 작업 시작: {}", LocalDateTime.now());

        try {
            // 오늘과 미래 날짜의 회의만 대상
            LocalDate today = LocalDate.now();
            List<Meeting> meetings = meetingRepository.findByDateGreaterThanEqual(today);
            logger.info("DB에서 조회된 미래 회의 수: {}", meetings.size());

            // Redis의 모든 미래 예약 데이터 초기화
            roomTimeSlotService.clearFutureTimeSlots(today);

            // 초기화 후 잠시 대기 (Redis 부하 분산을 위해)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            logger.info("Redis 미래 예약 데이터 초기화 완료");

            if (meetings.isEmpty()) {
                logger.info("미래 예약된 회의가 없어 동기화 작업을 종료합니다.");
                return;
            }

            // DB 데이터를 기반으로 Redis 재구성 (배치 처리)
            final int BATCH_SIZE = 50;
            int processedCount = 0;
            int batchCount = 0;
            int successCount = 0;
            int failCount = 0;

            // 배치 처리로 Redis 부하 분산
            for (int i = 0; i < meetings.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, meetings.size());
                List<Meeting> batch = meetings.subList(i, endIndex);
                batchCount++;

                int batchSuccess = processMeetingBatch(batch);
                successCount += batchSuccess;
                failCount += (batch.size() - batchSuccess);
                processedCount += batch.size();

                logger.info("배치 처리 진행 상태: {}/{} (성공: {}/{}, 실패: {})",
                        batchCount, (meetings.size() + BATCH_SIZE - 1) / BATCH_SIZE,
                        successCount, processedCount, failCount);

                // 배치 간 짧은 대기 시간 추가
                if (i + BATCH_SIZE < meetings.size()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            logger.info("Redis-DB 전체 재동기화 작업 완료: 총 {}개 회의 중 {}개 처리 성공, {}개 실패, {}",
                    meetings.size(), successCount, failCount, LocalDateTime.now());
        } catch (Exception e) {
            logger.error("Redis-DB 전체 재동기화 작업 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 회의 배치를 처리하여 Redis에 동기화
     * 
     * @param meetings 처리할 회의 목록
     * @return 성공적으로 처리된 회의 수
     */
    private int processMeetingBatch(List<Meeting> meetings) {
        int successCount = 0;
        int failCount = 0;

        for (Meeting meeting : meetings) {
            try {
                Room room = meeting.getRoom();
                LocalDate date = meeting.getDate();
                LocalTime startTime = meeting.getStartTime();
                LocalTime endTime = meeting.getEndTime();

                // Redis 시간 슬롯 예약 (DB 정보 기준)
                boolean reserved = roomTimeSlotService.reserveTimeRange(room.getRoomId(), date, startTime, endTime);

                if (reserved) {
                    successCount++;
                    logger.debug("회의실 예약 재동기화: 회의ID={}, 회의실={}, 날짜={}, 시간={}-{}",
                            meeting.getMeetingId(), room.getName(), date, startTime, endTime);
                } else {
                    failCount++;
                    logger.warn("회의실 예약 재동기화 실패: 회의ID={}, 회의실={}, 날짜={}, 시간={}-{}",
                            meeting.getMeetingId(), room.getName(), date, startTime, endTime);
                }
            } catch (Exception e) {
                failCount++;
                logger.error("회의 동기화 중 오류 (회의ID={}): {}", meeting.getMeetingId(), e.getMessage());
            }
        }

        logger.info("배치 처리 결과: 총 {}개 중 {}개 성공, {}개 실패",
                meetings.size(), successCount, failCount);
        return successCount;
    }
}