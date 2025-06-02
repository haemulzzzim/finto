package com.haemulzzzim.fintobe.meeting_room.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.haemulzzzim.fintobe.meeting_room.entity.Room;

@Repository
public interface MeetingRoomRepository extends JpaRepository<Room, Long> {
    boolean existsByName(String name);

    // 수용 인원 이상인 회의실만 조회
    @Query("SELECT r FROM Room r WHERE r.capacity >= :capacity")
    List<Room> findByCapacityGreaterThanEqual(@Param("capacity") int capacity);

    // 모든 회의실 ID 조회
    @Query("SELECT r.roomId FROM Room r")
    List<Long> findAllRoomIds();
}