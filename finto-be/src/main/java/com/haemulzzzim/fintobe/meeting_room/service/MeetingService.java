package com.haemulzzzim.fintobe.meeting_room.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.haemulzzzim.fintobe.exception.ErrorCode;
import com.haemulzzzim.fintobe.exception.business.EntityAlreadyExistException;
import com.haemulzzzim.fintobe.meeting_room.dto.MeetingRequestDto;
import com.haemulzzzim.fintobe.meeting_room.dto.MeetingResponseDto;
import com.haemulzzzim.fintobe.meeting_room.entity.Employee;
import com.haemulzzzim.fintobe.meeting_room.entity.Meeting;
import com.haemulzzzim.fintobe.meeting_room.entity.MeetingParticipant;
import com.haemulzzzim.fintobe.meeting_room.entity.Room;
import com.haemulzzzim.fintobe.meeting_room.repository.MeetingParticipantRepository;
import com.haemulzzzim.fintobe.meeting_room.repository.MeetingRepository;
import com.haemulzzzim.fintobe.meeting_room.repository.MeetingRoomRepository;
import com.haemulzzzim.fintobe.meeting_room.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MeetingService {
	private static final Logger logger = LoggerFactory.getLogger(MeetingService.class);

	private final MeetingRepository meetingRepository;
	private final UserRepository userRepository;
	private final MeetingRoomRepository meetingRoomRepository;
	private final MeetingParticipantRepository participantRepository;
	private final RoomTimeSlotService roomTimeSlotService;

	/**
	 * 시작/종료 시간 null 체크 및 종료시간 자동 세팅
	 * 
	 * @param startTime 시작 시간 (LocalTime)
	 * @param endTime   종료 시간 (nullable, LocalTime)
	 * @return [startTime, endTime] (endTime이 null이면 startTime+1시간)
	 */
	private LocalTime[] resolveTimes(LocalTime startTime, LocalTime endTime) {
		if (startTime == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작 시간은 필수입니다.");
		}
		if (endTime == null) {
			endTime = startTime.plusHours(1);
		}
		return new LocalTime[] { startTime, endTime };
	}

	private void validateTimeRange(LocalTime start, LocalTime end) {
		LocalTime min = LocalTime.of(7, 0);
		LocalTime max = LocalTime.of(20, 0);
		if (start.isBefore(min) || start.isAfter(max)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작시간은 07:00~20:00 사이여야 합니다.");
		}
		if (end.isBefore(min) || end.isAfter(max)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료시간은 07:00~20:00 사이여야 합니다.");
		}
		if (!end.isAfter(start)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료시간은 시작시간보다 늦어야 합니다.");
		}
	}

	private void validateFutureDateTime(LocalDate date, LocalTime startTime) {
		LocalDate today = LocalDate.now();
		LocalTime now = LocalTime.now();
		if (date.isBefore(today) || (date.isEqual(today) && !startTime.isAfter(now))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약은 현재 시각 이후만 가능합니다.");
		}
	}

	/**
	 * ID로 회의실 정보를 조회합니다.
	 */
	private Room findRoomById(Long roomId) {
		return meetingRoomRepository.findById(roomId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회의실 정보를 찾을 수 없습니다."));
	}

	/**
	 * ID로 직원 정보를 조회합니다.
	 */
	private Employee findEmployeeById(Long employeeId) {
		return userRepository.findById(employeeId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "직원 정보를 찾을 수 없습니다."));
	}

	/**
	 * ID로 회의 예약 정보를 조회합니다.
	 */
	private Meeting findMeetingById(Integer meetingId) {
		return meetingRepository.findById(meetingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약 정보를 찾을 수 없습니다."));
	}

	/**
	 * Google Calendar ID가 중복되지 않는지 확인합니다.
	 */
	private void validateGcIdUniqueness(String newGcId, String existingGcId) {
		if ((existingGcId == null || !existingGcId.equals(newGcId)) && meetingRepository.existsByGcId(newGcId)) {
			throw new EntityAlreadyExistException("이미 존재하는 Google Calendar ID입니다.", ErrorCode.USER_ALREADY_EXIST);
		}
	}

	/**
	 * 시간 유효성 검증 및 처리를 수행합니다.
	 */
	private void validateAndSetTimes(MeetingRequestDto dto) {
		LocalTime[] times = resolveTimes(dto.getStartTime(), dto.getEndTime());
		validateTimeRange(times[0], times[1]);
		validateFutureDateTime(dto.getDate(), times[0]);
		dto.setStartTime(times[0]);
		dto.setEndTime(times[1]);
	}

	// 예약 생성 (Redis 시간 슬롯 및 Meeting 엔티티, 참가자 정보를 한 트랜잭션으로 처리)
	@Transactional(isolation = Isolation.SERIALIZABLE)
	public MeetingResponseDto createMeeting(MeetingRequestDto dto) {
		logger.info("회의 예약 생성 시작 - 제목: {}, 날짜: {}, 시작: {}, 종료: {}",
				dto.getTitle(), dto.getDate(), dto.getStartTime(), dto.getEndTime());

		// 1. 기본 정보 및 중복 체크
		validateGcIdUniqueness(dto.getGcId(), null);

		// 2. 필수 엔티티 조회
		Employee host = findEmployeeById(dto.getHostEmpSeq());
		Room room = findRoomById(dto.getRoomId().longValue());

		// 3. 시간 유효성 검증 및 처리
		validateAndSetTimes(dto);

		// 4. 시간 슬롯 예약 시도 (한 번에 연속된 범위 예약)
		if (!roomTimeSlotService.reserveTimeRange(room.getRoomId(), dto.getDate(), dto.getStartTime(),
				dto.getEndTime())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 예약된 시간대가 포함되어 있습니다");
		}

		// 5. 중복 예약 체크 (겹치는 시간)
		List<Meeting> overlapped = meetingRepository
				.findByRoomAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
						room, dto.getDate(), dto.getEndTime(), dto.getStartTime());

		// 수정: 종료 시간과 시작 시간이 정확히 같은 경우는 겹치지 않는 것으로 판단
		overlapped = overlapped.stream()
				.filter(meeting -> !(meeting.getEndTime().equals(dto.getStartTime())
						|| meeting.getStartTime().equals(dto.getEndTime())))
				.toList();

		if (!overlapped.isEmpty()) {
			// 시간 슬롯 예약 취소 (롤백)
			roomTimeSlotService.cancelTimeRange(room.getRoomId(), dto.getDate(), dto.getStartTime(), dto.getEndTime());
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 해당 시간에 예약이 존재합니다");
		}

		// 6. Meeting 엔티티 저장
		Meeting meeting = createMeetingEntity(dto, room, host);
		Meeting saved = meetingRepository.save(meeting);

		// 7. 참가자 정보 처리 (요청에 있는 경우)
		saveParticipants(saved, host, dto.getParticipantEmails());

		logger.info("회의 예약 완료 - ID: {}, 회의실: {}, 날짜: {}",
				saved.getMeetingId(), room.getName(), dto.getDate());

		return toResponseDto(saved);
	}

	/**
	 * DTO로부터 Meeting 엔티티를 생성합니다.
	 */
	private Meeting createMeetingEntity(MeetingRequestDto dto, Room room, Employee host) {
		return Meeting.builder()
				.title(dto.getTitle())
				.date(dto.getDate())
				.minutePeriod(dto.getMinutePeriod())
				.startTime(dto.getStartTime())
				.endTime(dto.getEndTime())
				.gcId(dto.getGcId())
				.room(room)
				.host(host)
				.build();
	}

	// 참가자 저장
	private void saveParticipants(Meeting meeting, Employee host, List<String> emails) {
		logger.info("회의 참가자 저장 시작 - 회의 ID: {}, 호스트: {}, 이메일 수: {}",
				meeting.getMeetingId(), host.getName(), emails != null ? emails.size() : 0);

		// 참석자들을 저장 (이메일 리스트가 있는 경우)
		if (emails != null && !emails.isEmpty()) {
			// 기존 참석자 정보 조회 (중복 체크용)
			List<MeetingParticipant> existingParticipants = participantRepository.findByMeeting(meeting);
			List<Long> existingEmpSeqs = existingParticipants.stream()
					.map(p -> p.getEmployee().getEmpSeq())
					.toList();

			logger.info("기존 등록된 참가자 ID: {}", existingEmpSeqs);

			// 모든 이메일에 대해 참석자 추가
			final int[] addedCount = { 0 }; // 람다에서 참조하기 위해 배열 사용
			for (String email : emails) {
				userRepository.findByEmail(email).ifPresent(employee -> {
					// 이미 등록된 참가자는 건너뜀
					if (existingEmpSeqs.contains(employee.getEmpSeq())) {
						logger.debug("이미 등록된 참가자 건너뜀 - 이메일: {}, 이름: {}",
								email, employee.getName());
						return;
					}

					MeetingParticipant participant = MeetingParticipant.builder()
							.meeting(meeting)
							.employee(employee)
							.build();
					try {
						participantRepository.save(participant);
						addedCount[0]++; // 카운터 증가
						logger.debug("참가자 추가 - 이메일: {}, 이름: {}", email, employee.getName());
					} catch (Exception e) {
						logger.error("참가자 추가 중 오류 발생 - 이메일: {}, 오류: {}",
								email, e.getMessage());
					}
				});
			}
			logger.info("새로 추가된 참가자 수: {}", addedCount[0]);
		}

		logger.info("회의 참가자 저장 완료");
	}

	// 예약 수정
	@Transactional(isolation = Isolation.SERIALIZABLE)
	public MeetingResponseDto updateMeeting(Integer meetingId, MeetingRequestDto dto) {
		logger.info("회의 예약 수정 시작 - ID: {}, 제목: {}", meetingId, dto.getTitle());

		// 1. 기존 예약 정보 조회
		Meeting meeting = findMeetingById(meetingId);

		// 2. 필수 엔티티 조회
		Employee host = findEmployeeById(dto.getHostEmpSeq());
		Room room = findRoomById(dto.getRoomId().longValue());

		// 3. GC ID 유효성 확인
		validateGcIdUniqueness(dto.getGcId(), meeting.getGcId());

		// 4. 시간 유효성 확인
		validateAndSetTimes(dto);

		// 5. 시간 관련 예약 충돌 체크 및 시간 슬롯 처리 (현재 예약 제외)
		// 현재 회의와 동일한 회의실 + 날짜 + 시간 (슬롯 이동 또는 완전 겹침) 고려
		if (!handleTimeSlotUpdate(meeting, dto, room)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 예약된 시간대와 겹칩니다.");
		}

		// 중복 예약 체크 (겹치는 시간)
		List<Meeting> overlapped = meetingRepository
				.findByRoomAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
						room, dto.getDate(), dto.getEndTime(), dto.getStartTime());

		// 현재 수정 중인 예약은 제외
		overlapped.removeIf(m -> m.getMeetingId().equals(meeting.getMeetingId()));

		// 수정: 종료 시간과 시작 시간이 정확히 같은 경우는 겹치지 않는 것으로 판단
		overlapped = overlapped.stream()
				.filter(m -> !(m.getEndTime().equals(dto.getStartTime()) || m.getStartTime().equals(dto.getEndTime())))
				.collect(Collectors.toList());

		if (!overlapped.isEmpty()) {
			// 실패시 원래 시간대로 롤백
			roomTimeSlotService.reserveTimeRange(
					meeting.getRoom().getRoomId(),
					meeting.getDate(),
					meeting.getStartTime(),
					meeting.getEndTime());
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 해당 시간에 예약이 존재합니다");
		}

		// 6. 엔티티 업데이트 및 참가자 처리
		updateMeetingEntity(meeting, dto, room, host);
		updateParticipants(meeting, host, dto.getParticipantEmails());

		// 7. 저장 및 응답 반환
		Meeting updatedMeeting = meetingRepository.save(meeting);
		return toResponseDto(updatedMeeting);
	}

	/**
	 * 예약 시간 변경을 처리합니다.
	 * 
	 * @return 예약 성공 여부
	 */
	private boolean handleTimeSlotUpdate(Meeting meeting, MeetingRequestDto dto, Room room) {
		// 기존 예약된 시간대 취소
		roomTimeSlotService.cancelTimeRange(
				meeting.getRoom().getRoomId(),
				meeting.getDate(),
				meeting.getStartTime(),
				meeting.getEndTime());

		// 새로운 시간대 예약 시도
		boolean reservationSuccess = roomTimeSlotService.reserveTimeRange(
				room.getRoomId(), dto.getDate(), dto.getStartTime(), dto.getEndTime());

		if (!reservationSuccess) {
			// 실패시 원래 시간대로 롤백
			roomTimeSlotService.reserveTimeRange(
					meeting.getRoom().getRoomId(),
					meeting.getDate(),
					meeting.getStartTime(),
					meeting.getEndTime());
			return false;
		}

		return true;
	}

	/**
	 * 참가자 정보를 업데이트합니다.
	 */
	private void updateParticipants(Meeting meeting, Employee host, List<String> participantEmails) {
		if (participantEmails != null) {
			// 기존 참가자 삭제
			logger.info("회의 ID: {}의 기존 참가자 삭제 시작", meeting.getMeetingId());
			List<MeetingParticipant> existingParticipants = participantRepository.findByMeeting(meeting);
			logger.info("삭제 대상 참가자 수: {}", existingParticipants.size());

			participantRepository.deleteByMeeting(meeting);
			participantRepository.flush(); // 명시적 flush 수행

			// 삭제 확인
			List<MeetingParticipant> remainingParticipants = participantRepository.findByMeeting(meeting);
			logger.info("참가자 삭제 후 남은 참가자 수: {}", remainingParticipants.size());

			if (!remainingParticipants.isEmpty()) {
				logger.warn("참가자 삭제가 완전히 이루어지지 않았습니다. 남은 참가자: {}",
						remainingParticipants.stream().map(p -> p.getEmployee().getEmail()).toList());
			}

			// 새 참가자 추가 (호스트 포함)
			saveParticipants(meeting, host, participantEmails);
		}
	}

	// 엔티티 업데이트 로직 분리
	private void updateMeetingEntity(Meeting meeting, MeetingRequestDto dto, Room room, Employee host) {
		meeting.setTitle(dto.getTitle());
		meeting.setDate(dto.getDate());
		meeting.setMinutePeriod(dto.getMinutePeriod());
		meeting.setStartTime(dto.getStartTime());
		meeting.setEndTime(dto.getEndTime());
		meeting.setGcId(dto.getGcId());
		meeting.setRoom(room);
		meeting.setHost(host);
	}

	// 예약 삭제
	@Transactional
	public void deleteMeeting(Integer meetingId) {
		logger.info("회의 예약 삭제 시작 - ID: {}", meetingId);

		Meeting meeting = findMeetingById(meetingId);

		// 1. 참가자 정보 삭제
		participantRepository.deleteByMeeting(meeting);

		// 2. 예약된 시간 슬롯 해제
		roomTimeSlotService.cancelTimeRange(
				meeting.getRoom().getRoomId(),
				meeting.getDate(),
				meeting.getStartTime(),
				meeting.getEndTime());

		// 3. 회의 정보 삭제
		meetingRepository.deleteById(meetingId);

		logger.info("회의 예약 삭제 완료 - ID: {}", meetingId);
	}

	// 예약 id로 조회
	@Transactional(readOnly = true)
	public MeetingResponseDto getMeetingById(Integer meetingId) {
		Meeting meeting = findMeetingById(meetingId);
		return toResponseDto(meeting);
	}

	// host(직원) id로 예약 목록 조회
	@Transactional(readOnly = true)
	public List<MeetingResponseDto> getMeetingsByEmployeeSeq(Long employeeSeq) {
		Employee employee = findEmployeeById(employeeSeq);
		List<Meeting> meetings = meetingRepository.findByHost(employee);
		return meetings.stream().map(this::toResponseDto).toList();
	}

	// host(직원) id로 예약 목록 페이지 조회
	@Transactional(readOnly = true)
	public Page<MeetingResponseDto> getMeetingPageByEmployeeSeq(Long employeeSeq, Pageable pageable) {
		Employee employee = findEmployeeById(employeeSeq);
		Page<Meeting> meetingsPage = meetingRepository.findByHost(employee, pageable);
		return meetingsPage.map(this::toResponseDto);
	}

	@Transactional(readOnly = true)
	public Page<MeetingResponseDto> getMeetingPage(Pageable pageable) {
		return meetingRepository.findAll(pageable).map(this::toResponseDto);
	}

	/**
	 * 모든 미팅 목록을 조회합니다.
	 * 
	 * @return 모든 미팅 목록
	 */
	@Transactional(readOnly = true)
	public List<MeetingResponseDto> getAllMeetings() {
		List<Meeting> meetings = meetingRepository.findAll();
		return meetings.stream().map(this::toResponseDto).toList();
	}

	/**
	 * 특정 회의실과 날짜의 모든 미팅 목록을 조회합니다.
	 * 
	 * @param roomId 회의실 ID
	 * @param date   날짜
	 * @return 미팅 목록
	 */
	@Transactional(readOnly = true)
	public List<Meeting> getMeetingsByRoomAndDate(Long roomId, LocalDate date) {
		Room room = findRoomById(roomId);
		return meetingRepository.findByRoomAndDate(room, date);
	}

	private MeetingResponseDto toResponseDto(Meeting meeting) {
		// 참가자 정보 조회 및 변환
		List<MeetingParticipant> participants = participantRepository.findByMeeting(meeting);
		logger.info("회의 ID: {}의 참석자 정보 조회 결과 - {}명", meeting.getMeetingId(), participants.size());

		// 참석자 목록 생성
		List<MeetingResponseDto.ParticipantDto> participantDtos = new ArrayList<>();

		// 모든 참석자 정보 로깅 (호스트는 이미 참석자 테이블에 포함되어 있음)
		// Employee host = meeting.getHost();

		// 모든 참석자 정보 로깅
		for (MeetingParticipant participant : participants) {
			Employee emp = participant.getEmployee();
			logger.info("참석자 조회: ID={}, 이름={}, 이메일={}",
					emp.getEmpSeq(), emp.getName(), emp.getEmail());

			// DTO 변환하여 목록에 추가
			participantDtos.add(MeetingResponseDto.ParticipantDto.builder()
					.empSeq(emp.getEmpSeq())
					.name(emp.getName())
					.email(emp.getEmail())
					.build());
		}

		// 최종 참석자 목록 로깅
		logger.info("최종 참석자 목록 ({}명):", participantDtos.size());
		for (MeetingResponseDto.ParticipantDto p : participantDtos) {
			logger.info("• 참석자: {} ({}) - ID: {}", p.getName(), p.getEmail(), p.getEmpSeq());
		}

		String hostName = meeting.getHost().getName();
		// String hostEmail = meeting.getHost().getEmail();

		return MeetingResponseDto.builder()
				.meetingId(meeting.getMeetingId())
				.title(meeting.getTitle())
				.date(meeting.getDate())
				.minutePeriod(meeting.getMinutePeriod())
				.startTime(meeting.getStartTime())
				.endTime(meeting.getEndTime())
				.gcId(meeting.getGcId())
				.roomId(meeting.getRoom().getRoomId())
				.hostEmpSeq(meeting.getHost().getEmpSeq())
				.roomName(meeting.getRoom().getName())
				.hostName(hostName)
				.userName(hostName) // 호스트 이름 사용
				.participants(participantDtos)
				.build();
	}

	/**
	 * 참석자 이메일 문자열을 리스트로 파싱합니다.
	 * 
	 * @param attendees 콤마로 구분된 이메일 문자열
	 * @return 파싱된 이메일 리스트
	 */
	public List<String> parseAttendeeEmails(String attendees) {
		logger.info("이메일 파싱 시작 - 원본 문자열: [{}], 길이: {}", attendees, attendees != null ? attendees.length() : 0);

		if (attendees == null || attendees.trim().isEmpty()) {
			logger.info("이메일 문자열이 비어있음, 빈 목록 반환");
			return new ArrayList<>();
		}

		List<String> result = Arrays.stream(attendees.split(","))
				.map(String::trim)
				.filter(email -> !email.isEmpty())
				.toList();

		logger.info("이메일 파싱 결과 - 개수: {}, 목록: {}", result.size(), result);
		return result;
	}

	/**
	 * 참석자 문자열로부터 실제 인원수를 계산합니다.
	 * 
	 * @param attendees 콤마로 구분된 참석자 문자열
	 * @return 참석자 수 (기존 호스트 자동 추가 제거)
	 */
	public int countAttendees(String attendees) {
		logger.info("참석자 수 계산 시작 - 원본 문자열: [{}], 길이: {}", attendees, attendees != null ? attendees.length() : 0);

		// 빈 문자열이나 null인 경우 0 반환 (기존 호스트 카운트 제거)
		if (attendees == null || attendees.trim().isEmpty()) {
			logger.info("참석자 목록이 비어있음, 0명");
			return 0;
		}

		// 이메일 목록 파싱
		List<String> emailList = parseAttendeeEmails(attendees);
		if (emailList.isEmpty()) {
			logger.info("파싱된 이메일 목록이 비어있음, 0명");
			return 0;
		}

		// 유효한 사용자 이메일만 필터링하여 계산
		List<String> validEmails = new ArrayList<>();
		for (String email : emailList) {
			boolean isValid = userRepository.findByEmail(email).isPresent();
			logger.info("이메일 유효성 확인: {} - {}", email, isValid ? "유효함" : "유효하지 않음");
			if (isValid) {
				validEmails.add(email);
			}
		}

		int totalCount = validEmails.size();
		logger.info("총 참석자 수 계산 결과 - 유효한 이메일: {}개", totalCount);

		return totalCount;
	}

}