package com.haemulzzzim.fintobe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    private final AppConfig appConfig;
    private final DataSource dataSource;

    @PostConstruct
    public void init() {
        log.info("프록시 경로: {}", appConfig.getProxyPath());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String loginProcessingUrl = appConfig.getProxyPath() + "/login";
        String loginPage = appConfig.getProxyPath() + "/page/users/login";
        String logoutUrl = appConfig.getProxyPath() + "/logout";
        String logoutSuccessUrl = appConfig.getProxyPath() + "/page/users/login?logout";

        http
                .csrf(csrf -> csrf.disable()) // CSRF 비활성화
                .cors(cors -> cors.configure(http)) // CORS 설정 활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**").permitAll()
                        .requestMatchers(loginPage, "/login", "/logout").permitAll()
                        .requestMatchers("/api/**").permitAll() // API 엔드포인트는 인증 없이 접근 가능
                        .requestMatchers("/meeting-room/**").authenticated() // 회의실 관련 URL은 인증 필요
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage(loginPage)
                        .loginProcessingUrl(loginProcessingUrl)
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl(loginPage + "?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl(logoutUrl)
                        .logoutSuccessUrl(logoutSuccessUrl)
                        .permitAll());
        // 필요에 따라 추가 설정 가능
        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.addAllowedOrigin("*"); // 모든 출처 허용
        configuration.addAllowedMethod("*"); // 모든 HTTP 메소드 허용 (GET, POST, PUT, DELETE 등)
        configuration.addAllowedHeader("*"); // 모든 헤더 허용
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}