package com.haemulzzzim.fintobe.meeting_room.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.haemulzzzim.fintobe.meeting_room.entity.Meeting;
import com.haemulzzzim.fintobe.meeting_room.entity.MeetingParticipant;

@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    // 특정 회의의 모든 참가자 조회
    List<MeetingParticipant> findByMeeting(Meeting meeting);

    // 특정 회의에서 참가자 삭제 (회의 취소 시 필요)
    @Transactional
    void deleteByMeeting(Meeting meeting);
}