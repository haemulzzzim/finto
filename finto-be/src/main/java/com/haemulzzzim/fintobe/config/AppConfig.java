package com.haemulzzzim.fintobe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

/**
 * 애플리케이션 설정 값을 관리하는 클래스
 */
@Configuration
@Getter
public class AppConfig {

    /**
     * Nginx reverse proxy 경로 접두사
     * application.yml의 app.proxy.path에서 값을 가져옵니다.
     */
    @Value("${app.proxy.path:/}")
    private String proxyPath;

    /**
     * 프론트엔드에서 사용할 경로 접두사
     * application.yml의 app.proxy.front-path에서 값을 가져옵니다.
     */
    @Value("${app.proxy.front-path:/service}")
    private String frontProxyPath;
}