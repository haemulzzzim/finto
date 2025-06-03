package com.haemulzzzim.fintobe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ThymeleafConfig implements WebMvcConfigurer {

	private final AppConfig appConfig;

	/**
	 * 전역 Thymeleaf 변수 설정
	 */
	@Bean
	public HandlerInterceptor thymeleafVariablesInterceptor() {
		return new HandlerInterceptor() {
			@Override
			public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
				jakarta.servlet.http.HttpServletResponse response,
				Object handler) {
				// 모든 요청에 proxyPath를 추가
				request.setAttribute("proxyPath", appConfig.getProxyPath());
				return true;
			}
		};
	}

	/**
	 * 인터셉터 등록
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(thymeleafVariablesInterceptor());
	}
}