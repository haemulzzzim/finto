package com.haemulzzzim.fintobe.meeting_room.service;

import com.haemulzzzim.fintobe.meeting_room.dto.MeetingRequestDto;
import com.haemulzzzim.fintobe.meeting_room.dto.MeetingResponseDto;
import com.haemulzzzim.fintobe.meeting_room.entity.Employee;
import com.haemulzzzim.fintobe.meeting_room.entity.Meeting;
import com.haemulzzzim.fintobe.meeting_room.entity.Room;
import com.haemulzzzim.fintobe.meeting_room.entity.Dept;
import com.haemulzzzim.fintobe.meeting_room.repository.MeetingRepository;
import com.haemulzzzim.fintobe.meeting_room.repository.UserRepository;
import com.haemulzzzim.fintobe.meeting_room.repository.MeetingRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MeetingServiceTest {
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MeetingRoomRepository meetingRoomRepository;

    @InjectMocks
    private MeetingService meetingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createMeeting_성공() {
        MeetingRequestDto dto = new MeetingRequestDto();
        dto.setTitle("테스트 회의");
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setStartTime("10:00");
        dto.setEndTime("11:00");
        dto.setMinutePeriod("60");
        dto.setGcId("test-gc-id");
        dto.setRoomId(1);
        dto.setHostEmpSeq(1L);

        Employee host = Employee.builder().empSeq(1).name("홍길동").build();
        Room room = Room.builder().roomId(1).name("A룸").build();
        Meeting meeting = Meeting.builder().meetingId(1).title(dto.getTitle()).room(room).host(host).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(meetingRoomRepository.findById(1)).thenReturn(Optional.of(room));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

        MeetingResponseDto response = meetingService.createMeeting(dto);
        assertThat(response.getTitle()).isEqualTo("테스트 회의");
        assertThat(response.getRoomName()).isEqualTo("A룸");
        assertThat(response.getHostName()).isEqualTo("홍길동");
    }

    @Test
    void createMeeting_호스트없음_예외() {
        MeetingRequestDto dto = new MeetingRequestDto();
        dto.setHostEmpSeq(1L);
        dto.setRoomId(1);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> meetingService.createMeeting(dto))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void updateMeeting_성공() {
        MeetingRequestDto dto = new MeetingRequestDto();
        dto.setTitle("수정 회의");
        dto.setDate(LocalDate.now().plusDays(2));
        dto.setStartTime("12:00");
        dto.setEndTime("13:00");
        dto.setMinutePeriod("60");
        dto.setGcId("update-gc-id");
        dto.setRoomId(2);
        dto.setHostEmpSeq(2L);

        Meeting meeting = Meeting.builder().meetingId(1).build();
        Employee host = Employee.builder().empSeq(2).name("김철수").build();
        Room room = Room.builder().roomId(2).name("B룸").build();

        when(meetingRepository.findById(1)).thenReturn(Optional.of(meeting));
        when(userRepository.findById(2L)).thenReturn(Optional.of(host));
        when(meetingRoomRepository.findById(2)).thenReturn(Optional.of(room));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

        MeetingResponseDto response = meetingService.updateMeeting(1, dto);
        assertThat(response.getTitle()).isEqualTo("수정 회의");
        assertThat(response.getRoomName()).isEqualTo("B룸");
        assertThat(response.getHostName()).isEqualTo("김철수");
    }

    @Test
    void deleteMeeting_성공() {
        when(meetingRepository.existsById(1)).thenReturn(true);
        doNothing().when(meetingRepository).deleteById(1);
        meetingService.deleteMeeting(1);
        verify(meetingRepository, times(1)).deleteById(1);
    }

    @Test
    void deleteMeeting_존재하지않음_예외() {
        when(meetingRepository.existsById(1)).thenReturn(false);
        assertThatThrownBy(() -> meetingService.deleteMeeting(1))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void getMeetingById_성공() {
        Meeting meeting = Meeting.builder().meetingId(1).title("조회 회의").build();
        when(meetingRepository.findById(1)).thenReturn(Optional.of(meeting));
        MeetingResponseDto response = meetingService.getMeetingById(1);
        assertThat(response.getTitle()).isEqualTo("조회 회의");
    }

    @Test
    void getMeetingById_존재하지않음_예외() {
        when(meetingRepository.findById(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> meetingService.getMeetingById(1))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void createMeeting_회의실없음_예외() {
        MeetingRequestDto dto = new MeetingRequestDto();
        dto.setHostEmpSeq(1L);
        dto.setRoomId(99); // 없는 회의실 ID
        Employee host = Employee.builder().empSeq(1).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(meetingRoomRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.createMeeting(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("회의실 정보를 찾을 수 없습니다");
    }

    @Test
    void createMeeting_중복GCID_예외() {
        MeetingRequestDto dto = new MeetingRequestDto();
        dto.setHostEmpSeq(1L);
        dto.setRoomId(1);
        dto.setGcId("DUPLICATE-GC-ID");
        Employee host = Employee.builder().empSeq(1).build();
        Room room = Room.builder().roomId(1).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(meetingRoomRepository.findById(1)).thenReturn(Optional.of(room));
        when(meetingRepository.save(any(Meeting.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("중복"));

        assertThatThrownBy(() -> meetingService.createMeeting(dto))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void updateMeeting_존재하지않는예약_예외() {
        MeetingRequestDto dto = new MeetingRequestDto();
        when(meetingRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingService.updateMeeting(999, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("예약 정보를 찾을 수 없습니다");
    }

    @Test
    void deleteMeeting_이미삭제된예약_예외() {
        when(meetingRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> meetingService.deleteMeeting(999))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("예약 정보를 찾을 수 없습니다");
    }

    @Test
    void createMeeting_필수값누락_예외() {
        MeetingRequestDto dto = new MeetingRequestDto(); // 아무 값도 세팅하지 않음
        assertThatThrownBy(() -> meetingService.createMeeting(dto))
                .isInstanceOf(ResponseStatusException.class);
        // 또는 ValidationException 등 실제 발생하는 예외로 변경
    }

    @Test
    void createMeeting_경계값_정상() {
        MeetingRequestDto dto = new MeetingRequestDto();
        dto.setTitle("경계값 회의");
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setStartTime("00:00");
        dto.setEndTime("23:59");
        dto.setMinutePeriod("1440");
        dto.setGcId("boundary-gc-id");
        dto.setRoomId(1);
        dto.setHostEmpSeq(1L);

        Employee host = Employee.builder().empSeq(1).name("홍길동").build();
        Room room = Room.builder().roomId(1).name("A룸").build();
        Meeting meeting = Meeting.builder().meetingId(1).title(dto.getTitle()).room(room).host(host).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(meetingRoomRepository.findById(1)).thenReturn(Optional.of(room));
        when(meetingRepository.save(any(Meeting.class))).thenReturn(meeting);

        MeetingResponseDto response = meetingService.createMeeting(dto);
        assertThat(response.getTitle()).isEqualTo("경계값 회의");
    }

    @Test
    void createMeeting_회의실중복예약_예외() {
        MeetingRequestDto dto = new MeetingRequestDto();
        dto.setTitle("중복 예약");
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setStartTime("10:00");
        dto.setEndTime("11:00");
        dto.setMinutePeriod("60");
        dto.setGcId("dup-gc-id");
        dto.setRoomId(1);
        dto.setHostEmpSeq(1L);

        Dept dept = Dept.builder().deptId(1).name("개발팀").build();
        Employee host = Employee.builder().empSeq(1).name("홍길동").dept(dept).build();
        Room room = Room.builder().roomId(1).name("A룸").dept(dept).build();
        Meeting existingMeeting = Meeting.builder().meetingId(2).room(room).startTime("10:00").endTime("11:00")
                .date(dto.getDate()).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(meetingRoomRepository.findById(1)).thenReturn(Optional.of(room));
        when(meetingRepository.findByRoomAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(room,
                dto.getDate(), dto.getEndTime(), dto.getStartTime()))
                .thenReturn(List.of(existingMeeting));

        assertThatThrownBy(() -> meetingService.createMeeting(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 해당 시간에 예약이 존재합니다");
    }

    @Test
    void createMeeting_잘못된시간포맷_예외() {
        MeetingRequestDto dto = new MeetingRequestDto();
        dto.setTitle("잘못된 시간");
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setStartTime("25:00"); // 잘못된 시간
        dto.setEndTime("26:00"); // 잘못된 시간
        dto.setMinutePeriod("60");
        dto.setGcId("badtime-gc-id");
        dto.setRoomId(1);
        dto.setHostEmpSeq(1L);

        Dept dept = Dept.builder().deptId(1).name("개발팀").build();
        Employee host = Employee.builder().empSeq(1).name("홍길동").dept(dept).build();
        Room room = Room.builder().roomId(1).name("A룸").dept(dept).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(meetingRoomRepository.findById(1)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> meetingService.createMeeting(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("시작/종료 시간 포맷이 올바르지 않습니다");
    }

    @Test
    void createMeeting_부서불일치_예외() {
        MeetingRequestDto dto = new MeetingRequestDto();
        dto.setTitle("부서 불일치");
        dto.setDate(LocalDate.now().plusDays(1));
        dto.setStartTime("10:00");
        dto.setEndTime("11:00");
        dto.setMinutePeriod("60");
        dto.setGcId("dept-gc-id");
        dto.setRoomId(1);
        dto.setHostEmpSeq(1L);

        Dept userDept = Dept.builder().deptId(1).name("개발팀").build();
        Dept roomDept = Dept.builder().deptId(2).name("영업팀").build();
        Employee host = Employee.builder().empSeq(1).name("홍길동").dept(userDept).build();
        Room room = Room.builder().roomId(1).name("A룸").dept(roomDept).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(host));
        when(meetingRoomRepository.findById(1)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> meetingService.createMeeting(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("예약자와 회의실의 부서가 일치하지 않습니다");
    }
}