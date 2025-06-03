package com.haemulzzzim.fintobe.meeting_room.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.haemulzzzim.fintobe.meeting_room.entity.Employee;

@Repository
public interface UserRepository extends JpaRepository<Employee, Long> {

	boolean existsByEmpno(String empno);

	boolean existsByEmail(String email);

	Optional<Employee> findByEmail(String email);

	Optional<Employee> findByEmpno(String empno);

}
