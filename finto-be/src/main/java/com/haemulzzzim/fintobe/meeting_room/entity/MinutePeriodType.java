package com.haemulzzzim.fintobe.meeting_room.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MinutePeriodType {

	FIFTEEN("15"),
	THIRTY("30"),
	FOURTYFIVE("45"),
	SIXTY("60"),
	;

	private final String period;

}
