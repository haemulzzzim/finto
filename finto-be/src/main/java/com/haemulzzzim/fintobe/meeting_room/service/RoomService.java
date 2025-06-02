package com.haemulzzzim.fintobe.meeting_room.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.haemulzzzim.fintobe.exception.ErrorCode;
import com.haemulzzzim.fintobe.exception.business.EntityAlreadyExistException;
import com.haemulzzzim.fintobe.meeting_room.dto.MeetingRoomResponse;
import com.haemulzzzim.fintobe.meeting_room.dto.RoomResponse;
import com.haemulzzzim.fintobe.meeting_room.dto.RoomResponseDto;
import com.haemulzzzim.fintobe.meeting_room.dto.RoomTimeSlot;
import com.haemulzzzim.fintobe.meeting_room.dto.TimeSlotDto;
import com.haemulzzzim.fintobe.meeting_room.entity.Meeting;
import com.haemulzzzim.fintobe.meeting_room.entity.Room;
import com.haemulzzzim.fintobe.meeting_room.repository.MeetingRepository;
import com.haemulzzzim.fintobe.meeting_room.repository.MeetingRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);
    private static final int START_HOUR = 7;
    private static final int END_HOUR = 21;
    private static final int SLOT_MINUTES = 30;

    private final MeetingRoomRepository meetingRoomRepository;
    private final MeetingRepository meetingRepository;
    private final RoomTimeSlotService roomTimeSlotService;

    /**
     * 모든 회의실 정보와 각 회의실의 가용 시간 슬롯을 조회합니다.
     * 
     * @param date 조회할 날짜
     * @return 회의실 정보 리스트
     */
    public List<RoomResponse> getAllRoomsWithTimeSlots(LocalDate date) {
        List<Room> rooms = meetingRoomRepository.findAll();
        return rooms.stream()
                .map(room -> convertToRoomResponse(room, date))
                .toList();
    }

    /**
     * 모든 회의실 정보와 각 회의실의 가용 시간 슬롯을 조회합니다. (MeetingRoomResponse 반환)
     * 
     * @param date 조회할 날짜
     * @return 회의실 정보 리스트
     */
    public List<MeetingRoomResponse> getAllRoomsWithTimeSlotsAsMeetingRoomResponse(LocalDate date) {
        List<Room> rooms = meetingRoomRepository.findAll();
        return rooms.stream()
                .map(room -> {
                    MeetingRoomResponse response = new MeetingRoomResponse(room);
                    response.setSlots(generateSimpleTimeSlots(date, room.getRoomId()));
                    return response;
                })
                .toList();
    }

    /**
     * Room 엔티티를 RoomResponse DTO로 변환
     */
    private RoomResponse convertToRoomResponse(Room room, LocalDate date) {
        RoomResponse response = new RoomResponse(room);
        List<RoomTimeSlot> timeSlots = generateRoomTimeSlots(room.getRoomId(), date);
        response.setTimeSlots(timeSlots);
        return response;
    }

    /**
     * RoomTimeSlot 객체 리스트를 생성합니다.
     * Redis Set에서 예약된 슬롯 정보를 가져와 룸 타임 슬롯 리스트로 변환합니다.
     * 
     * @param roomId 회의실 ID
     * @param date   날짜
     * @return RoomTimeSlot 리스트
     */
    private List<RoomTimeSlot> generateRoomTimeSlots(Long roomId, LocalDate date) {
        // 예약된 시간 슬롯 목록 조회
        Set<String> reservedSlots = roomTimeSlotService.getReservedSlots(roomId, date);

        List<RoomTimeSlot> timeSlots = new ArrayList<>();
        LocalTime current = LocalTime.of(START_HOUR, 0);

        while (current.getHour() < END_HOUR) {
            LocalTime startTime = current;
            LocalTime endTime = current.plusMinutes(SLOT_MINUTES);

            String timeSlotStr = formatTimeSlot(startTime);
            boolean isAvailable = !reservedSlots.contains(timeSlotStr);

            RoomTimeSlot slot = new RoomTimeSlot();
            slot.setRoomId(roomId);
            slot.setStartTime(startTime);
            slot.setEndTime(endTime);
            slot.setAvailable(isAvailable);

            timeSlots.add(slot);
            current = endTime;
        }

        return timeSlots;
    }

    /**
     * 시간 슬롯을 생성합니다. (MeetingRoomResponse.TimeSlot 형식)
     * 
     * @param date   날짜
     * @param roomId 회의실 ID
     * @return 시간 슬롯 리스트
     */
    private List<MeetingRoomResponse.TimeSlot> generateSimpleTimeSlots(LocalDate date, Long roomId) {
        // 예약된 시간 슬롯 목록 조회
        Set<String> reservedSlots = roomTimeSlotService.getReservedSlots(roomId, date);

        List<MeetingRoomResponse.TimeSlot> slots = new ArrayList<>();
        for (int hour = START_HOUR; hour < END_HOUR; hour++) {
            for (int minute = 0; minute < 60; minute += SLOT_MINUTES) {
                String timeLabel = String.format("%02d:%02d~%02d:%02d",
                        hour, minute,
                        minute == 30 ? hour + 1 : hour,
                        minute == 30 ? 0 : minute + 30);

                String timeSlotStr = String.format("%02d:%02d", hour, minute);
                boolean isReserved = reservedSlots.contains(timeSlotStr);

                slots.add(new MeetingRoomResponse.TimeSlot(timeLabel, isReserved));
            }
        }
        return slots;
    }

    /**
     * 특정 회의실 정보와 가용 시간 슬롯을 조회합니다.
     * 
     * @param roomId 회의실 ID
     * @param date   조회할 날짜
     * @return 회의실 정보
     */
    public RoomResponse getRoomWithTimeSlots(Long roomId, LocalDate date) {
        Room room = findRoomById(roomId);
        return convertToRoomResponse(room, date);
    }

    /**
     * 회의실 ID로 회의실을 조회합니다.
     */
    private Room findRoomById(Long roomId) {
        return meetingRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "회의실 정보를 찾을 수 없습니다."));
    }

    /**
     * 회의실이 지정된 인원수를 수용할 수 있는지 확인합니다.
     */
    private boolean hasCapacity(Long roomId, int attendees) {
        Room room = findRoomById(roomId);
        boolean result = room.getCapacity() >= attendees;
        logger.info("회의실 수용 인원 확인 - 회의실: {}({}), 수용인원: {}, 요청인원: {}, 결과: {}",
                room.getRoomId(), room.getName(), room.getCapacity(), attendees, result);
        return result;
    }

    /**
     * 특정 시간 범위의 슬롯들이 Redis에서 예약 가능한지 확인합니다.
     */
    private boolean areTimeSlotsFreeInRedis(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        logger.info("Redis 시간 슬롯 확인 시작 - 회의실: {}, 날짜: {}, 시간: {}~{}", roomId, date, startTime, endTime);
        LocalTime current = startTime;
        while (current.isBefore(endTime)) {
            String timeSlotStr = formatTimeSlot(current);
            boolean isAvailable = roomTimeSlotService.isSlotAvailable(roomId, date, timeSlotStr);
            logger.debug("시간 슬롯 확인 - 회의실: {}, 시간: {}, 가용여부: {}", roomId, timeSlotStr, isAvailable);
            if (!isAvailable) {
                logger.info("Redis 시간 슬롯 예약 불가 감지 - 회의실: {}, 날짜: {}, 시간: {}", roomId, date, timeSlotStr);
                return false;
            }
            current = current.plusMinutes(SLOT_MINUTES);
        }
        logger.info("Redis 시간 슬롯 확인 완료 - 회의실: {}, 날짜: {}, 시간: {}~{}, 결과: 가능", roomId, date, startTime, endTime);
        return true;
    }

    /**
     * 특정 시간대에 회의실이 예약 가능한지 확인합니다. (조회용)
     * Redis만 확인하여 빠른 응답 시간을 제공합니다.
     */
    public boolean isTimeSlotAvailableForQuery(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime,
            int attendees) {
        logger.info("회의실 빠른 가용성 확인 시작 - 회의실: {}, 날짜: {}, 시간: {}~{}, 인원: {}",
                roomId, date, startTime, endTime, attendees);

        // 1. 수용 인원 확인
        if (!hasCapacity(roomId, attendees)) {
            logger.info("회의실 수용 인원 부족으로 예약 불가 - 회의실: {}, 요청인원: {}", roomId, attendees);
            return false;
        }

        // 2. Redis에서만 시간 슬롯 가용성 확인 (조회 성능 최적화)
        boolean redisResult = areTimeSlotsFreeInRedis(roomId, date, startTime, endTime);
        logger.info("회의실 빠른 가용성 확인 완료 - 회의실: {}, 결과: {}", roomId, redisResult);

        return redisResult;
    }

    /**
     * 특정 시간대에 회의실이 예약 가능한지 확인합니다.
     * 데이터베이스와 Redis 모두 검사하여 예약 가능 여부를 판단합니다.
     */
    public boolean isTimeSlotAvailable(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime,
            int attendees) {
        // 1. 수용 인원 확인
        if (!hasCapacity(roomId, attendees)) {
            return false;
        }

        // 2. Redis에서 가용 여부 확인
        boolean redisAvailable = areTimeSlotsFreeInRedis(roomId, date, startTime, endTime);

        // 3. 데이터베이스에서 가용 여부 확인
        boolean dbAvailable = checkDatabaseAvailability(roomId, date, startTime, endTime);

        // Redis와 DB 간의 불일치 감지 및 처리
        if (redisAvailable != dbAvailable) {
            Set<String> unavailableSlots = new HashSet<>();
            Set<String> conflictSlots = new HashSet<>();

            handleRedisDatabaseInconsistency(roomId, date, redisAvailable, dbAvailable, unavailableSlots,
                    conflictSlots);
        }

        return redisAvailable && dbAvailable;
    }

    /**
     * 특정 시간대에 회의실이 예약 가능한지 확인합니다 (수정 시).
     * 지정된 예약 ID를 제외하고 검사합니다.
     * 데이터베이스와 Redis 모두 검사하여 예약 가능 여부를 판단합니다.
     */
    public boolean isTimeSlotAvailableForUpdate(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime,
            int attendees, Integer excludeMeetingId) {
        // 1. 수용 인원 확인
        if (!hasCapacity(roomId, attendees)) {
            return false;
        }

        // 2. Redis에서 가용 여부 확인 (임시로 작업 수행)
        // 해당 시간대 슬롯이 이미 예약되어 있을 수 있으나, 현재 수정 중인 예약에 의한 것일 수 있음
        // 따라서 DB 체크에 의존하고 Redis는 나중에 업데이트됨

        // 3. 데이터베이스에서 가용 여부 확인 (현재 수정 중인 예약 제외)
        Room room = findRoomById(roomId);
        List<Meeting> overlappedMeetings = meetingRepository
                .findByRoomAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                        room, date, endTime, startTime);

        // 현재 수정 중인 예약은 제외
        overlappedMeetings = overlappedMeetings.stream()
                .filter(meeting -> !meeting.getMeetingId().equals(excludeMeetingId))
                .toList();

        // 종료 시간과 시작 시간이 정확히 같은 경우는 겹치지 않는 것으로 판단
        overlappedMeetings = overlappedMeetings.stream()
                .filter(meeting -> !(meeting.getEndTime().equals(startTime) || meeting.getStartTime().equals(endTime)))
                .toList();

        return overlappedMeetings.isEmpty();
    }

    /**
     * 데이터베이스에서 해당 시간대에 예약이 있는지 확인합니다.
     */
    private boolean checkDatabaseAvailability(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Room room = findRoomById(roomId);
        List<Meeting> overlapped = meetingRepository
                .findByRoomAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
                        room, date, endTime, startTime);

        // 종료 시간과 시작 시간이 정확히 같은 경우는 겹치지 않는 것으로 판단
        overlapped = overlapped.stream()
                .filter(meeting -> !(meeting.getEndTime().equals(startTime) || meeting.getStartTime().equals(endTime)))
                .toList();

        return overlapped.isEmpty();
    }

    /**
     * 시간 겹침 여부를 확인합니다.
     */
    private boolean hasTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        // 수정된 판정 로직:
        // 1. 시간 슬롯이 정확히 경계를 이루는 경우 (예: 10:30~11:00와 11:00~11:30)는 겹치지 않음으로 판단
        // 2. 종료 시간과 시작 시간이 같은 경우 겹치지 않는 것으로 판정
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * Redis와 DB 간 불일치를 처리합니다.
     */
    private void handleRedisDatabaseInconsistency(Long roomId, LocalDate date, boolean redisAvailable,
            boolean dbAvailable, Set<String> unavailableSlots, Set<String> conflictSlots) {
        if (redisAvailable != dbAvailable) {
            logger.warn("회의실 가용성 불일치 발견: roomId={}, date={}, Redis={}, DB={}",
                    roomId, date, redisAvailable, dbAvailable);

            if (dbAvailable && !redisAvailable) {
                // DB는 가능하지만 Redis는 불가능 - Redis 슬롯 해제
                logger.info("Redis 예약 슬롯 해제 (DB와 일치시킴): {}", unavailableSlots);
                for (String slot : unavailableSlots) {
                    roomTimeSlotService.freeTimeSlot(roomId, date, slot);
                }
            } else if (!dbAvailable && redisAvailable) {
                // DB는 불가능하지만 Redis는 가능 - Redis 슬롯 예약
                logger.info("Redis 예약 슬롯 설정 (DB와 일치시킴): {}", conflictSlots);
                for (String slot : conflictSlots) {
                    roomTimeSlotService.reserveTimeSlot(roomId, date, slot);
                }
            }
        }
    }

    /**
     * 조회 성능을 위해 Redis만 사용하여 가용 회의실을 찾습니다.
     */
    public List<RoomResponse> getAvailableRoomsForQuery(LocalDate date, LocalTime startTime, LocalTime endTime,
            int attendeesCount) {
        logger.info("조회용 가용 회의실 검색 시작 - 날짜: {}, 시간: {}~{}, 인원: {}", date, startTime, endTime, attendeesCount);
        long startTimeMillis = System.currentTimeMillis();

        // 1. 수용 인원 기준으로 필터링된 회의실 가져오기
        List<Room> capacityFilteredRooms = meetingRoomRepository.findByCapacityGreaterThanEqual(attendeesCount);

        if (capacityFilteredRooms.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 수용 인원을 만족하는 회의실 ID 목록 추출
        List<Long> roomIds = capacityFilteredRooms.stream()
                .map(Room::getRoomId)
                .toList();

        // 3. Redis로만 예약 가능한 회의실 ID 필터링 (병렬 처리)
        Set<Long> availableRoomIds = roomTimeSlotService.getAvailableRoomIdsParallel(date, startTime, endTime, roomIds);

        if (availableRoomIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 기존 예약과 시간 충돌 확인 (필터링된 회의실에 대해서만)
        List<Meeting> existingMeetings = meetingRepository.findAllByDateAndRoomIds(date,
                new ArrayList<>(availableRoomIds));

        // 회의실별로 예약 목록 그룹화 및 병렬 처리
        Map<Long, List<Meeting>> meetingsByRoomId = existingMeetings.stream()
                .collect(Collectors.groupingBy(meeting -> meeting.getRoom().getRoomId()));

        // 시간 충돌이 없는 회의실만 필터링 (병렬 처리)
        Set<Long> conflictRoomIds = meetingsByRoomId.entrySet().parallelStream()
                .filter(entry -> {
                    List<Meeting> meetings = entry.getValue();
                    boolean hasConflict = meetings.stream().anyMatch(meeting -> hasTimeOverlap(startTime, endTime,
                            meeting.getStartTime(), meeting.getEndTime()));
                    return hasConflict;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // 충돌이 있는 회의실 제외
        availableRoomIds.removeAll(conflictRoomIds);

        if (availableRoomIds.isEmpty()) {
            logger.info("시간 충돌 검사 후 가용 회의실이 없습니다.");
            return new ArrayList<>();
        }

        // 5. 가용 회의실 데이터 구성 (병렬 처리)
        List<RoomResponse> result = capacityFilteredRooms.parallelStream()
                .filter(room -> availableRoomIds.contains(room.getRoomId()))
                .map(room -> convertToRoomResponse(room, date))
                .toList();

        long endTimeMillis = System.currentTimeMillis();
        logger.info("조회용 가용 회의실 검색 완료 - 소요시간: {}ms, 결과: {}개",
                endTimeMillis - startTimeMillis, result.size());

        return result;
    }

    // 시간 형식 포맷팅 (HH:mm)
    private String formatTimeSlot(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    /**
     * 디버깅: 특정 회의실의 예약된 슬롯을 조회합니다.
     * 
     * @param roomId 회의실 ID
     * @param date   날짜
     * @return 예약된 시간 슬롯 Set
     */
    public Set<String> getReservedSlots(Long roomId, LocalDate date) {
        return roomTimeSlotService.getReservedSlots(roomId, date);
    }

    /**
     * 지정된 날짜 이후의 모든 미래 시간 슬롯 데이터를 초기화합니다.
     */

    /**
     * 회의실을 생성합니다.
     * 
     * @param room 회의실 정보
     * @return 생성된 회의실 정보
     */
    @Transactional
    public Room createRoom(Room room) {
        if (meetingRoomRepository.existsByName(room.getName())) {
            throw new EntityAlreadyExistException("이미 존재하는 회의실명입니다.", ErrorCode.USER_ALREADY_EXIST);
        }
        return meetingRoomRepository.save(room);
    }

    /**
     * 회의실 정보를 수정합니다.
     * 
     * @param id   회의실 ID
     * @param room 수정할 회의실 정보
     * @return 수정된 회의실 정보
     */
    @Transactional
    public Room updateRoom(Long id, Room room) {
        Room existing = findRoomById(id);

        if (!existing.getName().equals(room.getName()) &&
                meetingRoomRepository.existsByName(room.getName())) {
            throw new EntityAlreadyExistException("이미 존재하는 회의실명입니다.", ErrorCode.USER_ALREADY_EXIST);
        }

        existing.setName(room.getName());
        existing.setLocation(room.getLocation());
        existing.setCapacity(room.getCapacity());

        return meetingRoomRepository.save(existing);
    }

    /**
     * 회의실을 삭제합니다.
     * 
     * @param id 회의실 ID
     */
    @Transactional
    public void deleteRoom(Long id) {
        if (!meetingRoomRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "회의실 정보를 찾을 수 없습니다.");
        }
        meetingRoomRepository.deleteById(id);
    }

    /**
     * 특정 시간대에 예약 가능한 회의실을 병렬 처리로 최적화하여 반환합니다.
     * 
     * @param date           날짜
     * @param startTime      시작 시간
     * @param endTime        종료 시간
     * @param attendeesCount 참석자 수
     * @return 예약 가능한 회의실 목록
     */
    public List<RoomResponse> getAvailableRooms(LocalDate date, LocalTime startTime, LocalTime endTime,
            int attendeesCount) {
        return getAvailableRoomsOptimized(date, startTime, endTime, attendeesCount);
    }

    /**
     * 특정 시간대에 예약 가능한 회의실을 필터링하여 반환합니다. (병렬 처리 최적화 버전)
     * 
     * @param date           날짜
     * @param startTime      시작 시간
     * @param endTime        종료 시간
     * @param attendeesCount 참석자 수
     * @return 예약 가능한 회의실 목록
     */
    public List<RoomResponse> getAvailableRoomsOptimized(LocalDate date, LocalTime startTime, LocalTime endTime,
            int attendeesCount) {
        logger.info("최적화된 가용 회의실 조회 시작 - 날짜: {}, 시간: {}~{}, 인원: {}", date, startTime, endTime, attendeesCount);
        long startTimeMillis = System.currentTimeMillis();

        // 1. 수용 인원 기준으로 필터링된 회의실 가져오기
        List<Room> capacityFilteredRooms = meetingRoomRepository.findByCapacityGreaterThanEqual(attendeesCount);

        if (capacityFilteredRooms.isEmpty()) {
            logger.info("수용 인원을 만족하는 회의실이 없습니다.");
            return new ArrayList<>();
        }

        // 2. 수용 인원을 만족하는 회의실 ID 목록 추출
        List<Long> roomIds = capacityFilteredRooms.stream()
                .map(Room::getRoomId)
                .toList();

        // 3. 각 시간 슬롯에 대해 예약 가능한 회의실 ID 필터링 (병렬 처리)
        Set<Long> availableRoomIds = roomTimeSlotService.getAvailableRoomIdsParallel(date, startTime, endTime, roomIds);

        if (availableRoomIds.isEmpty()) {
            logger.info("시간 슬롯 검사 중 가용 회의실이 모두 소진되었습니다.");
            return new ArrayList<>();
        }

        // 4. 기존 예약과 시간 충돌 확인 (필터링된 회의실에 대해서만)
        List<Meeting> existingMeetings = meetingRepository.findAllByDateAndRoomIds(date,
                new ArrayList<>(availableRoomIds));

        // 회의실별로 예약 목록 그룹화 및 병렬 처리
        Map<Long, List<Meeting>> meetingsByRoomId = existingMeetings.stream()
                .collect(Collectors.groupingBy(meeting -> meeting.getRoom().getRoomId()));

        // 시간 충돌이 없는 회의실만 필터링 (병렬 처리)
        Set<Long> conflictRoomIds = meetingsByRoomId.entrySet().parallelStream()
                .filter(entry -> {
                    List<Meeting> meetings = entry.getValue();
                    boolean hasConflict = meetings.stream().anyMatch(meeting -> hasTimeOverlap(startTime, endTime,
                            meeting.getStartTime(), meeting.getEndTime()));
                    return hasConflict;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // 충돌이 있는 회의실 제외
        availableRoomIds.removeAll(conflictRoomIds);

        if (availableRoomIds.isEmpty()) {
            logger.info("시간 충돌 검사 후 가용 회의실이 없습니다.");
            return new ArrayList<>();
        }

        // 5. 가용 회의실 데이터 구성 (병렬 처리)
        List<RoomResponse> result = capacityFilteredRooms.parallelStream()
                .filter(room -> availableRoomIds.contains(room.getRoomId()))
                .map(room -> convertToRoomResponse(room, date))
                .toList();

        long endTimeMillis = System.currentTimeMillis();
        logger.info("최적화된 가용 회의실 조회 완료 - 소요시간: {}ms, 결과: {}개",
                endTimeMillis - startTimeMillis, result.size());

        return result;
    }

    /**
     * 특정 날짜, 시간에 예약 가능한 회의실 목록을 DTO로 조회합니다.
     */
    public List<RoomResponseDto> getAvailableRoomsDto(LocalDate date, LocalTime startTime, LocalTime endTime,
            int attendees) {
        // 모든 회의실 조회
        List<Room> allRooms = meetingRoomRepository.findAll();

        // 필터링 및 DTO 변환
        return allRooms.stream()
                .filter(room -> room.getCapacity() >= attendees) // 인원 수 필터링
                .map(room -> {
                    // 해당 회의실의 모든 시간 슬롯 조회
                    List<RoomTimeSlot> timeSlots = generateRoomTimeSlots(room.getRoomId(), date);

                    // 시간 슬롯 DTO 변환
                    List<TimeSlotDto> timeSlotDtos = timeSlots.stream()
                            .map(slot -> TimeSlotDto.builder()
                                    .startTime(slot.getStartTime())
                                    .endTime(slot.getEndTime())
                                    .available(slot.isAvailable())
                                    .build())
                            .toList();

                    // 해당 시간대 예약 가능 여부 확인
                    boolean isAvailable = isTimeSlotAvailableForQuery(
                            room.getRoomId(), date, startTime, endTime, attendees);

                    // RoomResponseDto 생성
                    return RoomResponseDto.builder()
                            .roomId(room.getRoomId())
                            .name(room.getName())
                            .capacity(room.getCapacity())
                            .location(room.getLocation())
                            .available(isAvailable)
                            .timeSlots(timeSlotDtos)
                            .build();
                })
                .filter(RoomResponseDto::isAvailable) // 가용한 회의실만 필터링
                .toList();
    }
}