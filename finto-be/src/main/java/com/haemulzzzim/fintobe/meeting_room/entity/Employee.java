package com.haemulzzzim.fintobe.meeting_room.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "Employee")
public class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "emp_seq", nullable = false)
	private Long empSeq;

	@Column(name = "empno", columnDefinition = "CHAR(6)", nullable = false, unique = true)
	@NotNull(message = "사원번호는 필수입니다.")
	@Pattern(regexp = "^\\d{6}$", message = "사원번호는 숫자 6자리여야 합니다.")
	private String empno;

	@Column(name = "name", length = 20, nullable = false)
	@NotBlank(message = "이름은 필수입니다.")
	@Size(max = 20, message = "이름은 최대 20자까지 입력할 수 있습니다.")
	private String name;

	@Column(name = "email", length = 255, nullable = false, unique = true)
	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 아닙니다.")
	@Size(max = 255, message = "이메일은 최대 255자까지 입력할 수 있습니다.")
	private String email;

	@Column(name = "password", length = 255, nullable = false)
	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(max = 255, message = "비밀번호는 최대 255자까지 입력할 수 있습니다.")
	private String password;

	@Column(name = "created_time", nullable = false)
	private LocalDateTime createdTime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "dept_id", nullable = false)
	private Dept dept;

	@PrePersist
	public void onCreate() {
		this.createdTime = LocalDateTime.now();
	}

}