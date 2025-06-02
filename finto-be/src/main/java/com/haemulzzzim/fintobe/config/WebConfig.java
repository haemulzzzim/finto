package com.haemulzzzim.fintobe.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// import com.haemulzzzim.fintobe.meeting_room.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // AuthInterceptor 제거: Spring Security만 사용
        // registry.addInterceptor(new AuthInterceptor())
        // .addPathPatterns("/**")
        // .excludePathPatterns("/login", "/api/auth/**", "/error", "/css/**", "/js/**",
        // "/images/**");
    }
}