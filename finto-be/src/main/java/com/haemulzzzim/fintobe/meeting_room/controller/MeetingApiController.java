package com.haemulzzzim.fintobe.meeting_room.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.haemulzzzim.fintobe.config.AppConfig;
import com.haemulzzzim.fintobe.meeting_room.dto.MeetingRequestDto;
import com.haemulzzzim.fintobe.meeting_room.dto.MeetingResponseDto;
import com.haemulzzzim.fintobe.meeting_room.service.CustomUserDetails;
import com.haemulzzzim.fintobe.meeting_room.service.MeetingService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회의 예약 API 컨트롤러
 */
@Controller
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
@Slf4j
public class MeetingApiController {
	private static final String DEFAULT_MINUTE_PERIOD = "30";
	private static final String DEFAULT_GC_ID = "1234567890";
	private static final Random random = new Random();
	private final MeetingService meetingService;
	private final AppConfig appConfig;
	private String REDIRECT_LOGIN_PATH;

	@PostConstruct
	public void init() {
		REDIRECT_LOGIN_PATH = "redirect:" + appConfig.getProxyPath() + "/page/users/login";
	}

	/**
	 * 랜덤 Google Calendar ID를 생성합니다.
	 * 26자리 랜덤 문자열 (숫자 + 대문자 + 소문자 + '-' 문자 포함)
	 *
	 * @return 랜덤 생성된 Google Calendar ID
	 */
	private String generateRandomGcId() {
		// GC ID 형식 기준: 26자, 알파벳 대소문자, 숫자, 하이픈(-)으로 구성
		final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-";
		final int GC_ID_LENGTH = 26;

		StringBuilder sb = new StringBuilder(GC_ID_LENGTH);
		for (int i = 0; i < GC_ID_LENGTH; i++) {
			int randomIndex = random.nextInt(ALLOWED_CHARS.length());
			sb.append(ALLOWED_CHARS.charAt(randomIndex));
		}

		log.debug("생성된 랜덤 Google Calendar ID: {}", sb);
		return sb.toString();
	}

	/**
	 * 회의실 예약을 처리합니다.
	 */
	@PostMapping("/reserve")
	public String reserveMeetingRoom(@RequestParam(value = "meetingId", required = false) Integer meetingId,
		@RequestParam("title") String title,
		@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		@RequestParam("startTime") @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
		@RequestParam("endTime") @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
		@RequestParam("roomId") Integer roomId,
		@RequestParam(value = "attendees", required = false, defaultValue = "") String attendees,
		RedirectAttributes redirectAttributes,
		@AuthenticationPrincipal CustomUserDetails user) {

		// 인증된 사용자가 없는 경우 처리
		if (user == null) {
			return REDIRECT_LOGIN_PATH;
		}

		try {
			log.info("회의실 예약 요청 처리 시작");
			log.info("• 예약 ID: {}", meetingId != null ? meetingId : "신규 예약");
			log.info("• 제목: {}", title);
			log.info("• 날짜: {}", date);
			log.info("• 시간: {} ~ {}", startTime, endTime);
			log.info("• 회의실ID: {}", roomId);
			log.info("• 예약자: {} (ID: {})", user.getUsername(), user.getUserSeq());
			log.info("• 참석자 문자열: [{}]", attendees);

			// 참석자 목록 파싱
			List<String> participantEmails = meetingService.parseAttendeeEmails(attendees);
			log.info("• 파싱된 참석자 ({} 명): {}", participantEmails.size(), participantEmails);

			// 예약 DTO 생성
			MeetingRequestDto requestDto = MeetingRequestDto.builder()
				.title(title)
				.date(date)
				.startTime(startTime)
				.endTime(endTime)
				.roomId(roomId)
				.hostEmpSeq(user.getUserSeq())
				.minutePeriod(DEFAULT_MINUTE_PERIOD)
				.gcId(generateRandomGcId())
				.participantEmails(participantEmails)
				.build();

			// 예약 처리 (신규 또는 수정)
			MeetingResponseDto result;
			if (meetingId != null) {
				// 기존 예약 수정
				result = meetingService.updateMeeting(meetingId, requestDto);
				log.info("예약 수정 완료 - ID: {}", result.getMeetingId());
				redirectAttributes.addFlashAttribute("message", "예약이 성공적으로 수정되었습니다.");
			} else {
				// 신규 예약
				result = meetingService.createMeeting(requestDto);
				log.info("예약 등록 완료 - ID: {}", result.getMeetingId());
				redirectAttributes.addFlashAttribute("message", "예약이 성공적으로 등록되었습니다.");
			}

			// 결과 참석자 정보 로깅
			if (result.getParticipants() != null) {
				log.info("저장된 참석자 정보 ({}명):", result.getParticipants().size());
				for (int i = 0; i < result.getParticipants().size(); i++) {
					MeetingResponseDto.ParticipantDto p = result.getParticipants().get(i);
					log.info("  참석자 #{}: {} ({}) - ID: {}", i + 1, p.getName(), p.getEmail(), p.getEmpSeq());
				}
			} else {
				log.warn("저장된 참석자 정보가 null입니다");
			}

			// 성공 페이지로 리다이렉트
			return "redirect:" + appConfig.getProxyPath() + "/page/meetings?tab=my";
		} catch (Exception e) {
			log.error("예약 처리 중 오류 발생", e);
			redirectAttributes.addFlashAttribute("error", "예약 처리 중 오류가 발생했습니다: " + e.getMessage());
			return "redirect:" + appConfig.getProxyPath() + "/page/meetings/reserve";
		}
	}

	/**
	 * 회의실 예약을 취소합니다.
	 */
	@DeleteMapping("/{id}")
	public String deleteMeeting(
		@PathVariable("id") Integer meetingId,
		@AuthenticationPrincipal CustomUserDetails user,
		RedirectAttributes redirectAttributes) {

		if (user == null) {
			return REDIRECT_LOGIN_PATH;
		}

		try {
			MeetingResponseDto meeting = meetingService.getMeetingById(meetingId);

			// 권한 확인 (본인 예약만 취소 가능)
			if (!meeting.getHostEmpSeq().equals(user.getUserSeq())) {
				log.warn("예약 취소 권한 없음 - 본인: {}, 예약자: {}", user.getUserSeq(), meeting.getHostEmpSeq());
				redirectAttributes.addFlashAttribute("error", "예약 취소 권한이 없습니다.");
				return "redirect:" + appConfig.getProxyPath() + "/page/meetings?tab=my";
			}

			// 예약 취소 처리
			meetingService.deleteMeeting(meetingId);
			redirectAttributes.addFlashAttribute("message", "예약이 성공적으로 취소되었습니다.");
			return "redirect:" + appConfig.getProxyPath() + "/page/meetings?tab=my";
		} catch (Exception e) {
			log.error("예약 삭제 중 오류 발생", e);
			redirectAttributes.addFlashAttribute("error", "예약 삭제 중 오류가 발생했습니다: " + e.getMessage());
			return "redirect:" + appConfig.getProxyPath() + "/page/meetings?tab=my";
		}
	}

	/**
	 * 내 예약 목록을 조회합니다. (API)
	 */
	@GetMapping("/my")
	@ResponseBody
	public ResponseEntity<?> getMyReservations(
		@RequestParam(value = "page", defaultValue = "0") int page,
		@RequestParam(value = "size", defaultValue = "10") int size,
		@AuthenticationPrincipal CustomUserDetails user) {

		if (user == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "로그인이 필요합니다."));
		}

		try {
			Pageable pageable = PageRequest.of(page, size);
			Page<MeetingResponseDto> reservationsPage = meetingService.getMeetingPageByEmployeeSeq(user.getUserSeq(),
				pageable);

			return ResponseEntity.ok(Map.of(
				"content", reservationsPage.getContent(),
				"currentPage", page,
				"totalPages", reservationsPage.getTotalPages(),
				"totalElements", reservationsPage.getTotalElements()));
		} catch (Exception e) {
			log.error("내 예약 목록 조회 중 오류 발생", e);
			return ResponseEntity.badRequest().body(Map.of("error", "예약 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	/**
	 * 모든 예약 목록을 조회합니다. (API)
	 */
	@GetMapping
	@ResponseBody
	public ResponseEntity<?> getAllReservations(
		@RequestParam(value = "page", defaultValue = "0") int page,
		@RequestParam(value = "size", defaultValue = "10") int size,
		@AuthenticationPrincipal CustomUserDetails user) {

		if (user == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "로그인이 필요합니다."));
		}

		try {
			Pageable pageable = PageRequest.of(page, size);
			Page<MeetingResponseDto> reservationsPage = meetingService.getMeetingPage(pageable);

			return ResponseEntity.ok(Map.of(
				"content", reservationsPage.getContent(),
				"currentPage", page,
				"totalPages", reservationsPage.getTotalPages(),
				"totalElements", reservationsPage.getTotalElements()));
		} catch (Exception e) {
			log.error("전체 예약 목록 조회 중 오류 발생", e);
			return ResponseEntity.badRequest().body(Map.of("error", "예약 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}
}