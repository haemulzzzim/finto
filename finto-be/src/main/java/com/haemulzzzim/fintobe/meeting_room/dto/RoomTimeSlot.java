package com.haemulzzzim.fintobe.meeting_room.dto;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomTimeSlot {
    private Long roomId;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isAvailable;
    private Long meetingId; // 예약된 경우 해당 미팅 ID

    public String getRedisKey() {
        return String.format("room:%d:timeslot:%s-%s", roomId,
                startTime.toString(), endTime.toString());
    }
}