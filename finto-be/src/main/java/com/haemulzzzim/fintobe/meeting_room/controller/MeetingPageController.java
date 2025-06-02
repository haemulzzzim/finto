package com.haemulzzzim.fintobe.meeting_room.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.haemulzzzim.fintobe.meeting_room.dto.MeetingResponseDto;
import com.haemulzzzim.fintobe.meeting_room.service.CustomUserDetails;
import com.haemulzzzim.fintobe.meeting_room.service.MeetingService;
import com.haemulzzzim.fintobe.meeting_room.service.RoomService;
import com.haemulzzzim.fintobe.meeting_room.service.RoomTimeSlotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회의실 예약 페이지 컨트롤러
 * 회의실 예약 관련 페이지 요청을 처리합니다.
 */
@Controller
@RequestMapping("/page/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingPageController {
    // 상수 정의
    private static final String REDIRECT_RESERVE_PATH = "redirect:/page/meetings/reserve";
    private static final String REDIRECT_MY_MEETINGS_PATH = "redirect:/page/meetings?tab=my";
    private static final String REDIRECT_LOGIN_PATH = "redirect:/page/users/login";
    private static final String MEETING_ROOM_RESERVE_VIEW = "meeting-room/reserve-room";
    private static final String MEETING_ROOM_LIST_VIEW = "meeting-room/meeting-room-list";
    private static final String DEFAULT_MINUTE_PERIOD = "30";
    private static final String DEFAULT_GC_ID = "1234567890";

    // 서비스 의존성
    private final MeetingService meetingService;
    private final RoomService roomService;
    private final RoomTimeSlotService roomTimeSlotService;

    /**
     * 회의실 예약 페이지를 보여줍니다.
     */
    @GetMapping("/reserve")
    public String showReservationFormPage(Model model, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return REDIRECT_LOGIN_PATH;
        }
        // model.addAttribute("rooms", roomService.getAllRoomsWithTimeSlots(LocalDate.now()));
        // model.addAttribute("meeting", null); // 신규 예약이므로 빈 객체 전달
        return MEETING_ROOM_RESERVE_VIEW;
    }

    /**
     * 회의실 예약 수정 페이지를 보여줍니다.
     */
    @GetMapping("/reserve/{id}")
    public String showEditReservationFormPage(@PathVariable("id") Integer meetingId, Model model,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return REDIRECT_LOGIN_PATH;
        }

        try {
            // 해당 예약 정보 조회
            log.info("예약 정보 조회 시작 - ID: {}", meetingId);
            MeetingResponseDto meeting = meetingService.getMeetingById(meetingId);
            log.info("예약 정보 조회 성공 - ID: {}, 제목: {}", meetingId, meeting.getTitle());

            // 세부 객체 정보 디버깅
            log.info("회의 객체 세부 정보:");
            log.info("• ID: {}", meeting.getMeetingId());
            log.info("• 제목: {}", meeting.getTitle());
            log.info("• 날짜: {}", meeting.getDate());
            log.info("• 시작시간: {}", meeting.getStartTime());
            log.info("• 종료시간: {}", meeting.getEndTime());
            log.info("• 시작시간 클래스: {}",
                    meeting.getStartTime() != null ? meeting.getStartTime().getClass().getName() : "null");
            log.info("• 시작시간 toString: {}",
                    meeting.getStartTime() != null ? meeting.getStartTime().toString() : "null");
            log.info("• 회의실 ID: {}", meeting.getRoomId());
            log.info("• 회의실명: {}", meeting.getRoomName());
            log.info("• 호스트 ID: {}", meeting.getHostEmpSeq());
            log.info("• 호스트명: {}", meeting.getHostName());
            log.info("• 호스트 이메일: {}", meeting.getUserName());

            // 참석자 정보 디버깅
            if (meeting.getParticipants() != null) {
                log.info("참석자 정보 ({}명):", meeting.getParticipants().size());
                for (int i = 0; i < meeting.getParticipants().size(); i++) {
                    MeetingResponseDto.ParticipantDto participant = meeting.getParticipants().get(i);
                    log.info("  참석자 #{}: {} ({}) - ID: {}",
                            i + 1,
                            participant.getName(),
                            participant.getEmail(),
                            participant.getEmpSeq());
                }
            } else {
                log.warn("참석자 목록이 null입니다! getMeetingById 결과를 확인하세요.");
            }

            // 권한 확인 (본인 예약만 수정 가능)
            if (!meeting.getHostEmpSeq().equals(user.getUserSeq())) {
                log.warn("예약 수정 권한 없음 - 본인: {}, 예약자: {}", user.getUserSeq(), meeting.getHostEmpSeq());
                return REDIRECT_MY_MEETINGS_PATH;
            }

            // 모든 회의실 목록 조회
            model.addAttribute("rooms", roomService.getAllRoomsWithTimeSlots(meeting.getDate()));

            // 참석자 목록을 쉼표로 구분된 문자열로 변환 - 참석자 이메일만 추출
            String attendees = meeting.getParticipantsAsString();

            log.info("수정 폼 데이터 준비 완료");
            log.info("• 회의 ID: {}", meeting.getMeetingId());
            log.info("• 제목: {}", meeting.getTitle());
            log.info("• 날짜: {}", meeting.getDate());
            log.info("• 시작시간: {}", meeting.getStartTime());
            log.info("• 종료시간: {}", meeting.getEndTime());
            log.info("• 회의실 ID: {}", meeting.getRoomId());
            log.info("• 회의실명: {}", meeting.getRoomName());
            log.info("• 참석자 문자열: {}", attendees);

            // 예약 정보와 참석자 정보 모델에 추가
            model.addAttribute("meeting", meeting);
            model.addAttribute("attendees", attendees); // 쉼표로 구분된 참석자 이메일 문자열
            model.addAttribute("isEdit", true);

            return MEETING_ROOM_RESERVE_VIEW;
        } catch (Exception e) {
            log.error("예약 수정 페이지 로드 중 오류 발생", e);
            return REDIRECT_MY_MEETINGS_PATH;
        }
    }

    /**
     * 회의실 예약 목록을 조회합니다.
     */
    @GetMapping
    public String getAllMeetingsPage(
            @RequestParam(value = "tab", defaultValue = "my") String tab,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model,
            @AuthenticationPrincipal CustomUserDetails user) {

        if (user == null) {
            return REDIRECT_LOGIN_PATH;
        }

        model.addAttribute("userName", user.getUsername());

        Pageable pageable = PageRequest.of(page, size);
        addMyReservationsToModel(model, user.getUserSeq(), pageable, page);

        if ("all".equals(tab)) {
            addAllReservationsToModel(model, pageable, page);
        }

        return MEETING_ROOM_LIST_VIEW;
    }

    // 유틸리티 메소드

    /**
     * 내 예약 목록을 모델에 추가합니다.
     */
    private void addMyReservationsToModel(Model model, Long userSeq, Pageable pageable, int page) {
        Page<MeetingResponseDto> myReservationsPage = meetingService.getMeetingPageByEmployeeSeq(userSeq, pageable);

        model.addAttribute("myReservations", myReservationsPage.getContent());
        model.addAttribute("myPage", page + 1);
        model.addAttribute("myTotalPages", myReservationsPage.getTotalPages());
    }

    /**
     * 전체 예약 목록을 모델에 추가합니다.
     */
    private void addAllReservationsToModel(Model model, Pageable pageable, int page) {
        Page<MeetingResponseDto> allReservationsPage = meetingService.getMeetingPage(pageable);

        model.addAttribute("allReservations", allReservationsPage.getContent());
        model.addAttribute("allPage", page + 1);
        model.addAttribute("allTotalPages", allReservationsPage.getTotalPages());
    }

    /**
     * 회의실 예약 상세 조회 페이지를 보여줍니다.
     */
    @GetMapping("/{id}")
    public String getMeetingByIdPage(@PathVariable("id") Integer meetingId, Model model,
            @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return REDIRECT_LOGIN_PATH;
        }

        try {
            MeetingResponseDto meeting = meetingService.getMeetingById(meetingId);
            model.addAttribute("meeting", meeting);
            model.addAttribute("userName", user.getUsername());

            // 참석자 목록을 쉼표로 구분된 문자열로 변환하여 추가
            String attendees = meeting.getParticipantsAsStringExcluding(user.getUsername());
            model.addAttribute("attendees", attendees);

            return "meeting-room/meeting-room-detail";
        } catch (Exception e) {
            log.error("회의실 예약 상세 조회 중 오류 발생", e);
            return REDIRECT_MY_MEETINGS_PATH;
        }
    }
}