package com.haemulzzzim.fintobe.meeting_room.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회의실 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDto {
    private Long roomId;
    private String name;
    private int capacity;
    private String location;
    private boolean available;
    private List<TimeSlotDto> timeSlots;
}