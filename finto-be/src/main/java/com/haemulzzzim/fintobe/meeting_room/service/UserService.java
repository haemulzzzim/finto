package com.haemulzzzim.fintobe.meeting_room.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.haemulzzzim.fintobe.exception.ErrorCode;
import com.haemulzzzim.fintobe.exception.business.EntityAlreadyExistException;
import com.haemulzzzim.fintobe.meeting_room.dto.UserDto;
import com.haemulzzzim.fintobe.meeting_room.entity.Employee;
import com.haemulzzzim.fintobe.meeting_room.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public Employee authenticate(String email, String password) {
		return userRepository.findByEmail(email)
				.filter(emp -> passwordEncoder.matches(password, emp.getPassword()))
				.orElse(null);
	}

	public Employee save(UserDto dto) {
		validateDuplicateEmpno(null, dto.getEmpno());
		validateDuplicateEmail(null, dto.getEmail());

		Employee employee = Employee.builder()
				.empno(dto.getEmpno())
				.name(dto.getName())
				.email(dto.getEmail())
				.password(passwordEncoder.encode(dto.getPassword()))
				.build();

		return userRepository.save(employee);
	}

	public List<Employee> findAll() {
		return userRepository.findAll();
	}

	public Optional<Employee> findById(Long id) {
		return userRepository.findById(id);
	}

	public void delete(Long id) {
		userRepository.deleteById(id);
	}

	public Employee update(Long id, UserDto dto) {
		Employee employee = userRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사원입니다."));
		validateDuplicateEmpno(employee.getEmpno(), dto.getEmpno());
		validateDuplicateEmail(employee.getEmail(), dto.getEmail());

		employee.setEmpno(dto.getEmpno());
		employee.setName(dto.getName());
		employee.setEmail(dto.getEmail());

		if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
			employee.setPassword(passwordEncoder.encode(dto.getPassword()));
		}

		return userRepository.save(employee);
	}

	private void validateDuplicateEmpno(String currentEmpno, String newEmpno) {
		if (!Objects.equals(currentEmpno, newEmpno) && userRepository.existsByEmpno(newEmpno)) {
			throw new EntityAlreadyExistException("이미 있는 사원번호입니다.", ErrorCode.USER_ALREADY_EXIST);
		}
	}

	private void validateDuplicateEmail(String currentEmail, String newEmail) {
		if (!Objects.equals(currentEmail, newEmail) && userRepository.existsByEmail(newEmail)) {
			throw new EntityAlreadyExistException("이미 있는 이메일입니다.", ErrorCode.USER_ALREADY_EXIST);
		}
	}

}
