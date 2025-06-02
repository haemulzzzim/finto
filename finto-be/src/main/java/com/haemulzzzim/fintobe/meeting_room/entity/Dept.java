package com.haemulzzzim.fintobe.meeting_room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "Dept")
public class Dept {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "dept_id", nullable = false)
	private Long deptId;

	@Column(name = "name", length = 20, nullable = false, unique = true)
	@NotBlank(message = "부서명은 필수입니다.")
	@Size(max = 20, message = "부서명은 최대 20자까지 입력할 수 있습니다.")
	private String name;

	@Column(name = "location", length = 2000, nullable = false)
	@NotBlank(message = "위치는 필수입니다.")
	@Size(max = 2000, message = "위치는 최대 2000자까지 입력할 수 있습니다.")
	private String location;

}
