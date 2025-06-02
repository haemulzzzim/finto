package com.haemulzzzim.fintobe.meeting_room.entity;

import java.time.LocalDate;
import java.time.LocalTime;

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "Meeting")
public class Meeting {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "meeting_id", nullable = false)
	private Long meetingId;

	@Column(name = "title", length = 100, nullable = false)
	@NotBlank(message = "회의 제목은 필수입니다.")
	@Size(max = 100, message = "회의 제목은 최대 100자까지 입력할 수 있습니다.")
	private String title;

	@Column(name = "date", nullable = false)
	@NotNull(message = "회의 날짜는 필수입니다.")
	private LocalDate date;

	@Column(name = "minute_period", nullable = false, columnDefinition = "varchar(255) default '60'")
	@NotBlank(message = "회의 시간 단위는 필수입니다.")
	@Builder.Default
	private String minutePeriod = "60";

	@Column(name = "start_time", nullable = false)
	@NotNull(message = "시작 시간은 필수입니다.")
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	@NotNull(message = "종료 시간은 필수입니다.")
	private LocalTime endTime;

	@Column(name = "gc_id", columnDefinition = "CHAR(26)", nullable = false, unique = true)
	@NotBlank(message = "Google Calendar ID는 필수입니다.")
	@Size(max = 26, message = "Google Calendar ID는 26자여야 합니다.")
	private String gcId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id")
	private Room room;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "host_emp_seq", nullable = false)
	@NotNull(message = "주최자 정보는 필수입니다.")
	private Employee host;

	@PrePersist
	public void prePersist() {
		if (this.minutePeriod == null) {
			this.minutePeriod = "60";
		}
	}

}
