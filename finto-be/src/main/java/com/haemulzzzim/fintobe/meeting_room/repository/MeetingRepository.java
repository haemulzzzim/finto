package com.haemulzzzim.fintobe.meeting_room.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.haemulzzzim.fintobe.meeting_room.entity.Employee;
import com.haemulzzzim.fintobe.meeting_room.entity.Meeting;
import com.haemulzzzim.fintobe.meeting_room.entity.Room;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {

    List<Meeting> findByHost(Employee host);

    Page<Meeting> findByHost(Employee host, Pageable pageable);

    List<Meeting> findByRoomAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(Room room, LocalDate date,
            LocalTime endTime, LocalTime startTime);

    boolean existsByGcId(String gcId);

    List<Meeting> findByRoomAndDate(Room room, LocalDate date);

    // 특정 날짜의 모든 회의 정보 조회
    @Query("SELECT m FROM Meeting m WHERE m.date = :date")
    List<Meeting> findAllByDate(@Param("date") LocalDate date);

    // 특정 날짜에 특정 회의실들에 대한 모든 회의 정보 조회
    @Query("SELECT m FROM Meeting m WHERE m.date = :date AND m.room.roomId IN :roomIds")
    List<Meeting> findAllByDateAndRoomIds(@Param("date") LocalDate date, @Param("roomIds") List<Long> roomIds);

    /**
     * 특정 날짜 이후의 모든 회의를 조회합니다.
     * 
     * @param date 기준 날짜
     * @return 해당 날짜 이후의 모든 회의 목록
     */
    List<Meeting> findByDateGreaterThanEqual(LocalDate date);

}