package com.haemulzzzim.fintobe.meeting_room.dto;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시간 슬롯 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDto {
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean available;
}