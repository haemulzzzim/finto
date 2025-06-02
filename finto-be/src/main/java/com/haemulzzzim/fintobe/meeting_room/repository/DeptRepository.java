package com.haemulzzzim.fintobe.meeting_room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.haemulzzzim.fintobe.meeting_room.entity.Dept;

@Repository
public interface DeptRepository extends JpaRepository<Dept, Integer> {
    boolean existsByName(String name);
}