package com.haemulzzzim.fintobe.meeting_room.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.haemulzzzim.fintobe.meeting_room.dto.RoomResponse;
import com.haemulzzzim.fintobe.meeting_room.entity.Meeting;
import com.haemulzzzim.fintobe.meeting_room.entity.Room;
import com.haemulzzzim.fintobe.meeting_room.repository.MeetingRepository;
import com.haemulzzzim.fintobe.meeting_room.repository.MeetingRoomRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RoomServicePerformanceTest {

    @Autowired
    private RoomService roomService;

    @Autowired
    private MeetingRoomRepository meetingRoomRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RoomTimeSlotService roomTimeSlotService;

    private static final int ROOM_COUNT = 500; // 회의실 수
    private static final int MEETING_COUNT = 1000; // 예약 수
    private static final LocalDate TEST_DATE = LocalDate.now();
    private static final LocalTime TEST_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime TEST_END_TIME = LocalTime.of(11, 0);
    private static final int TEST_ATTENDEES = 5;
    private static final Random random = new Random();

    @BeforeEach
    public void setup() {
        // 기존 데이터 삭제
        meetingRepository.deleteAll();
        meetingRoomRepository.deleteAll();

        // 대량의 회의실 생성
        List<Room> rooms = new ArrayList<>();
        for (int i = 1; i <= ROOM_COUNT; i++) {
            Room room = new Room();
            room.setName("회의실 " + i);
            room.setLocation("위치 " + i);
            room.setCapacity(random.nextInt(20) + 1); // 1~20명 수용 가능
            rooms.add(room);
        }
        meetingRoomRepository.saveAll(rooms);

        // 대량의 예약 생성
        List<Meeting> meetings = new ArrayList<>();
        List<Room> savedRooms = meetingRoomRepository.findAll();

        for (int i = 1; i <= MEETING_COUNT; i++) {
            Room room = savedRooms.get(random.nextInt(savedRooms.size()));

            // 랜덤 시간 설정 (7시~20시 30분 단위)
            int startHour = random.nextInt(14) + 7; // 7~20시
            int startMinute = random.nextInt(2) * 30; // 0분 또는 30분
            LocalTime meetingStart = LocalTime.of(startHour, startMinute);

            // 회의 길이 30분~2시간
            int durationSlots = random.nextInt(4) + 1; // 1~4 슬롯 (30분~2시간)
            LocalTime meetingEnd = meetingStart.plusMinutes(30 * durationSlots);

            // 랜덤 날짜 설정 (오늘 또는 내일)
            LocalDate meetingDate = TEST_DATE.plusDays(random.nextInt(2));

            Meeting meeting = new Meeting();
            meeting.setRoom(room);
            meeting.setDate(meetingDate);
            meeting.setStartTime(meetingStart);
            meeting.setEndTime(meetingEnd);
            meeting.setTitle("회의 " + i);
            meeting.setAttendeesCount(random.nextInt(room.getCapacity()) + 1);

            meetings.add(meeting);

            // Redis에도 예약 정보 저장
            LocalTime current = meetingStart;
            while (current.isBefore(meetingEnd)) {
                String timeSlotStr = String.format("%02d:%02d", current.getHour(), current.getMinute());
                roomTimeSlotService.markSlotAsReserved(room.getRoomId(), meetingDate, timeSlotStr);
                current = current.plusMinutes(30);
            }
        }

        meetingRepository.saveAll(meetings);
    }

    @Test
    public void testGetAvailableRoomsPerformance() {
        // 실행 시간 측정
        Instant start = Instant.now();

        List<RoomResponse> availableRooms = roomService.getAvailableRooms(
                TEST_DATE, TEST_START_TIME, TEST_END_TIME, TEST_ATTENDEES);

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        System.out.println("====== 성능 테스트 결과 ======");
        System.out.println("회의실 수: " + ROOM_COUNT);
        System.out.println("예약 수: " + MEETING_COUNT);
        System.out.println("테스트 검색 조건: 날짜=" + TEST_DATE + ", 시간=" + TEST_START_TIME + "-" + TEST_END_TIME + ", 참석자="
                + TEST_ATTENDEES);
        System.out.println("조회된 가용 회의실 수: " + availableRooms.size());
        System.out.println("실행 시간: " + duration.toMillis() + "ms (" + duration.getSeconds() + "."
                + duration.toMillisPart() + "초)");
        System.out.println("============================");
    }

    @Test
    public void comparePerformance() {
        // 최적화 전 테스트
        System.out.println("===== 최적화 전 성능 테스트 =====");
        Instant start1 = Instant.now();

        List<RoomResponse> originalRooms = roomService.getAvailableRooms(
                TEST_DATE, TEST_START_TIME, TEST_END_TIME, TEST_ATTENDEES);

        Instant end1 = Instant.now();
        Duration duration1 = Duration.between(start1, end1);

        System.out.println("조회된 가용 회의실 수: " + originalRooms.size());
        System.out.println("실행 시간: " + duration1.toMillis() + "ms (" + duration1.getSeconds() + "."
                + duration1.toMillisPart() + "초)");

        // 최적화 후 테스트
        System.out.println("\n===== 최적화 후 성능 테스트 =====");
        Instant start2 = Instant.now();

        List<RoomResponse> optimizedRooms = roomService.getAvailableRoomsOptimized(
                TEST_DATE, TEST_START_TIME, TEST_END_TIME, TEST_ATTENDEES);

        Instant end2 = Instant.now();
        Duration duration2 = Duration.between(start2, end2);

        System.out.println("조회된 가용 회의실 수: " + optimizedRooms.size());
        System.out.println("실행 시간: " + duration2.toMillis() + "ms (" + duration2.getSeconds() + "."
                + duration2.toMillisPart() + "초)");

        // 성능 향상 계산
        double improvementRatio = (double) duration1.toMillis() / duration2.toMillis();
        double improvementPercent = (improvementRatio - 1) * 100;

        System.out.println("\n===== 성능 비교 결과 =====");
        System.out.println("최적화 전: " + duration1.toMillis() + "ms");
        System.out.println("최적화 후: " + duration2.toMillis() + "ms");
        System.out.println("성능 향상: " + String.format("%.2f", improvementPercent) + "% (약 "
                + String.format("%.2f", improvementRatio) + "배 빠름)");

        // 두 결과가 동일한지 확인
        assertEquals(originalRooms.size(), optimizedRooms.size(), "최적화 전/후 결과 수가 일치해야 합니다.");

        // 같은 회의실이 포함되어 있는지 확인 (순서는 다를 수 있음)
        Set<Long> originalRoomIds = originalRooms.stream()
                .map(room -> room.getRoomId())
                .collect(Collectors.toSet());

        Set<Long> optimizedRoomIds = optimizedRooms.stream()
                .map(room -> room.getRoomId())
                .collect(Collectors.toSet());

        assertEquals(originalRoomIds, optimizedRoomIds, "최적화 전/후 결과에 포함된 회의실이 일치해야 합니다.");
    }
}