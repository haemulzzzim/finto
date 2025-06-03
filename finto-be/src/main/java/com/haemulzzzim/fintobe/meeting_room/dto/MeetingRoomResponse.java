package com.haemulzzzim.fintobe.meeting_room.dto;

import com.haemulzzzim.fintobe.meeting_room.entity.Room;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MeetingRoomResponse {

    private Long roomId;
    private String name;
    private Integer capacity;
    private List<TimeSlot> slots;

    public MeetingRoomResponse(Room room) {
        this.roomId = room.getRoomId();
        this.name = room.getName();
        this.capacity = room.getCapacity();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TimeSlot {
        private String label;
        private boolean selected;

        public TimeSlot(String label, boolean selected) {
            this.label = label;
            this.selected = selected;
        }
    }

}