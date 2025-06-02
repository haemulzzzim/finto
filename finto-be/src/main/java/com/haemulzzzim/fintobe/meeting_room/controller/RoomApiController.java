package com.haemulzzzim.fintobe.meeting_room.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.haemulzzzim.fintobe.meeting_room.dto.RoomResponse;
import com.haemulzzzim.fintobe.meeting_room.service.CustomUserDetails;
import com.haemulzzzim.fintobe.meeting_room.service.MeetingService;
import com.haemulzzzim.fintobe.meeting_room.service.RoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회의실 API 컨트롤러
 */
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomApiController {

    private final RoomService roomService;
    private final MeetingService meetingService;

    /**
     * 모든 회의실과 시간 슬롯 조회
     */
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRoomsWithTimeSlots(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails user) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(roomService.getAllRoomsWithTimeSlots(targetDate));
    }

    /**
     * 특정 시간대에 예약 가능한 회의실 조회 (레거시)
     */
    @GetMapping("/available/legacy")
    public ResponseEntity<List<RoomResponse>> getAvailableRoomsLegacy(
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(name = "endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(name = "attendees", required = false, defaultValue = "1") String attendees,
            @AuthenticationPrincipal CustomUserDetails user) {

        // attendees 문자열을 콤마(,)로 분리하고 인원수 계산
        int attendeesCount = meetingService.countAttendees(attendees);

        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<RoomResponse> availableRooms = roomService.getAvailableRooms(targetDate, startTime, endTime,
                attendeesCount);
        return ResponseEntity.ok(availableRooms);
    }

    /**
     * 특정 시간대에 예약 가능한 회의실 조회 (최적화 버전)
     */
    @GetMapping("/available-optimized")
    public List<RoomResponse> getAvailableRoomsOptimized(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam("attendees") String attendees) {
        // 디버그용 로깅 추가
        log.debug("attendees 원본 값: [{}], 길이: {}", attendees, attendees.length());
        log.debug("attendees 공백 제거 후: [{}], 길이: {}", attendees.trim(), attendees.trim().length());

        // attendees 문자열을 콤마(,)로 분리하고 인원수 계산
        int attendeesCount = meetingService.countAttendees(attendees);
        log.debug("계산된 참석자 수: {}", attendeesCount);

        // 조회 성능 최적화를 위해 Redis만 사용하는 메서드 호출
        return roomService.getAvailableRoomsForQuery(targetDate, startTime, endTime, attendeesCount);
    }

    /**
     * 특정 회의실과 시간 슬롯 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomWithTimeSlots(
            @PathVariable Long id,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails user) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(roomService.getRoomWithTimeSlots(id, targetDate));
    }

    /**
     * 특정 회의실이 특정 시간대에 예약 가능한지 확인합니다.
     */
    @GetMapping("/available/{roomId}")
    public boolean isRoomAvailable(
            @PathVariable("roomId") Long roomId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam("attendees") String attendees) {
        // attendees 문자열을 콤마(,)로 분리하고 인원수 계산
        int attendeesCount = meetingService.countAttendees(attendees);

        // 조회 성능 최적화를 위해 Redis만 사용하는 메서드 호출
        return roomService.isTimeSlotAvailableForQuery(roomId, targetDate, startTime, endTime, attendeesCount);
    }

    /**
     * 회의실 예약 처리 전에 최종 가용성을 확인합니다. (DB + Redis 모두 확인)
     */
    @PostMapping("/check-availability")
    public boolean checkAvailabilityForReservation(
            @RequestParam("roomId") Long roomId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam("attendees") String attendees) {
        // attendees 문자열을 콤마(,)로 분리하고 인원수 계산
        int attendeesCount = meetingService.countAttendees(attendees);

        // 예약 처리 시 DB와 Redis 모두 확인하는 메서드 호출
        return roomService.isTimeSlotAvailable(roomId, targetDate, startTime, endTime, attendeesCount);
    }

    /**
     * 회의실 예약 가능 여부를 확인합니다.
     */
    @GetMapping("/check-available")
    public boolean isRoomAvailableForQuery(
            @RequestParam("roomId") Integer roomId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(value = "attendees", required = false, defaultValue = "") String attendees) {

        log.info("[체크] 회의실 예약 가능 여부 API 요청 - roomId: {}, date: {}, time: {}~{}, attendees: [{}]",
                roomId, date, startTime, endTime, attendees);

        try {
            // URL 인코딩된 공백 문제 확인을 위해 디코딩 및 로깅
            String decodedAttendees = java.net.URLDecoder.decode(attendees, java.nio.charset.StandardCharsets.UTF_8);
            if (!attendees.equals(decodedAttendees)) {
                log.info("[체크] URL 디코딩 결과: [{}] -> [{}]", attendees, decodedAttendees);
                attendees = decodedAttendees; // 디코딩된 값 사용
            }
        } catch (Exception e) {
            log.warn("[체크] URL 디코딩 중 오류 발생, 원본 값 사용: {}", e.getMessage());
        }

        // 참석자 문자열을 인원수로 변환
        int attendeesCount = meetingService.countAttendees(attendees);

        log.info("[체크] 빠른 예약 가능 여부 확인 완료 - 회의실: {}, 날짜: {}, 시간: {}~{}, 인원: {}, 원본값: [{}]",
                roomId, date, startTime, endTime, attendeesCount, attendees);

        // 예약 가능 여부 확인
        boolean result = roomService.isTimeSlotAvailableForQuery(roomId.longValue(), date, startTime, endTime,
                attendeesCount);
        log.info("[체크] 예약 가능 여부 결과: {}", result);

        return result;
    }

    /**
     * 수정 가능 여부를 확인합니다.
     */
    @GetMapping("/check-update-available")
    public boolean isUpdateAvailable(
            @RequestParam("roomId") Integer roomId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam("meetingId") Integer meetingId) {

        // 자신의 예약인 경우에 해당 시간대에 다른 예약이 있는지 확인
        return roomService.isTimeSlotAvailableForUpdate(roomId.longValue(), date, startTime, endTime, 1, meetingId);
    }

    /**
     * 가용 회의실 목록을 조회합니다.
     */
    @GetMapping("/available")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(value = "attendees", required = false, defaultValue = "") String attendees) {

        log.info("[조회] 가용 회의실 목록 API 요청 - date: {}, time: {}~{}, attendees: [{}]",
                date, startTime, endTime, attendees);

        try {
            // URL 인코딩된 공백 문제 확인을 위해 디코딩 및 로깅
            String decodedAttendees = java.net.URLDecoder.decode(attendees, java.nio.charset.StandardCharsets.UTF_8);
            if (!attendees.equals(decodedAttendees)) {
                log.info("[조회] URL 디코딩 결과: [{}] -> [{}]", attendees, decodedAttendees);
                attendees = decodedAttendees; // 디코딩된 값 사용
            }
        } catch (Exception e) {
            log.warn("[조회] URL 디코딩 중 오류 발생, 원본 값 사용: {}", e.getMessage());
        }

        // 참석자 문자열을 인원수로 변환
        int attendeesCount = meetingService.countAttendees(attendees);

        log.info("[조회] 가용 회의실 목록 API 처리 - 날짜: {}, 시간: {}~{}, 인원: {}, 원본값: [{}]",
                date, startTime, endTime, attendeesCount, attendees);

        List<RoomResponse> availableRooms = roomService.getAvailableRooms(date, startTime, endTime, attendeesCount);

        log.info("[조회] 가용 회의실 조회 결과 - 개수: {}", availableRooms.size());

        return ResponseEntity.ok(availableRooms);
    }
}