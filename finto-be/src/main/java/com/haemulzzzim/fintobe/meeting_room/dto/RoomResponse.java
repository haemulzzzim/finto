package com.haemulzzzim.fintobe.meeting_room.dto;

import java.util.List;

import com.haemulzzzim.fintobe.meeting_room.entity.Room;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoomResponse {
    private Long roomId;
    private String name;
    private String location;
    private Integer capacity;
    private List<RoomTimeSlot> timeSlots;

    public RoomResponse(Room room) {
        this.roomId = room.getRoomId();
        this.name = room.getName();
        this.location = room.getLocation();
        this.capacity = room.getCapacity();
    }
}