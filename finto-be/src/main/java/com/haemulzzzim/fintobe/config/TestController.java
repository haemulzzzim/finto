package com.haemulzzzim.fintobe.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 설정값 테스트용 컨트롤러
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final AppConfig appConfig;

    /**
     * 프록시 경로 설정을 확인합니다.
     */
    @GetMapping("/config")
    public String testConfig() {
        log.info("프록시 경로 설정: {}", appConfig.getProxyPath());
        return "프록시 경로 설정: " + appConfig.getProxyPath();
    }
}