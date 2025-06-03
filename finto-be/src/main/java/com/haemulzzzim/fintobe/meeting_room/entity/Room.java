package com.haemulzzzim.fintobe.meeting_room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "Room")
public class Room {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long roomId;

	@NotBlank(message = "회의실 이름은 필수입니다.")
	@Size(max = 50, message = "회의실 이름은 최대 50자까지 입력 가능합니다.")
	@Column(nullable = false, unique = true)
	private String name;

	@NotBlank(message = "회의실 위치는 필수입니다.")
	@Size(max = 100, message = "회의실 위치는 최대 100자까지 입력 가능합니다.")
	@Column(nullable = false)
	private String location;

	@NotNull(message = "수용 인원은 필수입니다.")
	@Min(value = 1, message = "수용 인원은 최소 1명 이상이어야 합니다.")
	@Column(nullable = false)
	private Integer capacity;

}
