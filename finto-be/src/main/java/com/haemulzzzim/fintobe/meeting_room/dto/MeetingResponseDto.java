package com.haemulzzzim.fintobe.meeting_room.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MeetingResponseDto {
    private Long meetingId;
    private String title;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String minutePeriod;
    private String gcId;
    private Long roomId;
    private Long hostEmpSeq;
    private String roomName;
    private String hostName;
    private String userName;
    private List<ParticipantDto> participants;

    /**
     * 참석자 이메일 목록을 쉼표(,)로 구분된 문자열로 반환합니다.
     * 
     * @return 쉼표로 구분된 참석자 이메일 문자열
     */
    public String getParticipantsAsString() {
        if (participants == null || participants.isEmpty()) {
            return "";
        }

        return participants.stream()
                .map(ParticipantDto::getEmail)
                .collect(Collectors.joining(", "));
    }

    /**
     * 특정 사용자를 제외한 참석자 이메일 목록을 쉼표(,)로 구분된 문자열로 반환합니다.
     * 
     * @param excludeEmail 제외할 사용자 이메일
     * @return 쉼표로 구분된 참석자 이메일 문자열 (특정 사용자 제외)
     */
    public String getParticipantsAsStringExcluding(String excludeEmail) {
        if (participants == null || participants.isEmpty()) {
            return "";
        }
        return participants.stream()
                .map(ParticipantDto::getEmail)
                .filter(email -> !email.equals(excludeEmail))
                .collect(Collectors.joining(","));
    }

    /**
     * 호스트 이메일을 포함하여 모든 참석자 이메일 목록을 쉼표(,)로 구분된 문자열로 반환합니다.
     * 
     * @return 쉼표로 구분된 참석자 이메일 문자열 (호스트 포함)
     */
    public String getParticipantsWithHostAsString() {
        if (userName == null) {
            return getParticipantsAsString();
        }

        String result = getParticipantsAsString();
        if (result.isEmpty()) {
            return userName;
        } else {
            return userName + "," + result;
        }
    }

    /**
     * 특정 사용자를 제외하고 호스트 이메일을 포함한 참석자 이메일 목록을 쉼표(,)로 구분된 문자열로 반환합니다.
     * 특정 사용자가 호스트인 경우에도 호스트는 반환 값에 포함됩니다.
     * 
     * @param excludeEmail 제외할 사용자 이메일
     * @return 쉼표로 구분된 참석자 이메일 문자열 (호스트 포함, 특정 사용자 제외)
     */
    public String getParticipantsWithHostAsStringExcluding(String excludeEmail) {
        String result = getParticipantsAsStringExcluding(excludeEmail);

        // 호스트 이메일이 userName 필드에 있고, excludeEmail과 다르면 호스트 이메일 추가
        if (userName != null && !userName.equals(excludeEmail)) {
            if (result.isEmpty()) {
                return userName;
            } else {
                return userName + "," + result;
            }
        }

        return result;
    }

    @Getter
    @Builder
    public static class ParticipantDto {
        private Long empSeq;
        private String name;
        private String email;
    }
}