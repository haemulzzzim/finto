package com.haemulzzzim.fintobe.meeting_room.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MeetingRequestDto {
    private Integer meetingId;

    @NotBlank(message = "제목을 입력하세요.")
    private String title;

    @NotNull(message = "날짜를 입력하세요.")
    @FutureOrPresent(message = "오늘 이후의 날짜만 선택할 수 있습니다.")
    private LocalDate date;

    @NotNull(message = "시작 시간을 입력하세요.")
    private LocalTime startTime;

    private LocalTime endTime;

    @NotBlank(message = "회의 시간(분 단위)을 입력하세요.")
    private String minutePeriod;

    @NotBlank(message = "Google Calendar ID를 입력하세요.")
    private String gcId;

    @NotNull(message = "회의실 ID를 입력하세요.")
    private Integer roomId;

    @NotNull(message = "주최자(직원) ID를 입력하세요.")
    private Long hostEmpSeq;

    private List<String> participantEmails;
}