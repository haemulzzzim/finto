package com.haemulzzzim.fintobe.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Autowired
    private AppConfig appConfig;

    public CustomAuthenticationSuccessHandler() {
        // 기본 URL 설정 (초기값)
        setDefaultTargetUrl("/home");
    }

    // 빈 초기화 후 설정값 적용
    @PostConstruct
    public void initTargetUrl() {
        // 프록시 경로가 설정된 경우 기본 타겟 URL 업데이트
        if (appConfig != null) {
            setDefaultTargetUrl(appConfig.getProxyPath() + "/home");
            log.info("기본 리다이렉트 URL이 {}로 설정되었습니다.", getDefaultTargetUrl());
        }
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {

        log.info("Authentication Success Handler 실행");

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String targetUrl = null;

        if (savedRequest != null) {
            String redirectUrl = savedRequest.getRedirectUrl();
            // Chrome DevTools나 기타 시스템 요청 필터링
            if (!redirectUrl.contains(".well-known") && !redirectUrl.contains("/favicon.ico")) {
                targetUrl = redirectUrl;
                log.info("저장된 요청으로 리다이렉트: {}", targetUrl);
                requestCache.removeRequest(request, response);
            }
        }

        // 저장된 요청이 없거나 필터링된 경우
        if (targetUrl == null) {
            String referer = request.getHeader("Referer");
            // Referer가 있고 로그인 페이지가 아닌 경우에만 사용
            if (StringUtils.hasText(referer) && !referer.contains("/login")) {
                targetUrl = referer;
                log.info("이전 페이지로 리다이렉트: {}", targetUrl);
            } else {
                targetUrl = getDefaultTargetUrl();
                log.info("기본 페이지로 리다이렉트: {}", targetUrl);
            }
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}