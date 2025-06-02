package com.haemulzzzim.fintobe;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FintoBeApplication {

	public static void main(String[] args) {
		// 서버 시간대를 Asia/Seoul(한국 시간)으로 명시적 설정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(FintoBeApplication.class, args);
	}

}
