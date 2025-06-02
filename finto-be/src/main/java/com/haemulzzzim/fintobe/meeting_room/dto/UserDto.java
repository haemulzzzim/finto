package com.haemulzzzim.fintobe.meeting_room.dto;

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
public class UserDto {

	@NotNull(message = "사원번호는 필수입니다.")
	@Pattern(regexp = "^\\d{6}$", message = "사원번호는 숫자 6자리여야 합니다.")
	private String empno;

	@NotBlank(message = "이름은 필수입니다.")
	@Size(max = 20, message = "이름은 최대 20자까지 입력할 수 있습니다.")
	private String name;

	@NotBlank(message = "이메일은 필수입니다.")
	@Email(message = "이메일 형식이 아닙니다.")
	@Size(max = 255, message = "이메일은 최대 255자까지 입력할 수 있습니다.")
	private String email;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Size(max = 255, message = "비밀번호는 최대 255자까지 입력할 수 있습니다.")
	private String password;

}