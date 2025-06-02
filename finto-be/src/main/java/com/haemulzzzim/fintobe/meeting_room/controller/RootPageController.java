package com.haemulzzzim.fintobe.meeting_room.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 루트 페이지 컨트롤러
 * 이전 URL 패턴과의 호환성을 위한 리다이렉트를 제공합니다.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class RootPageController {

    // 루트 페이지 리다이렉트
    @GetMapping("/")
    public String root() {
        return "redirect:/page/users/home";
    }

    // 기존 로그아웃 URL에 대한 리다이렉트
    @GetMapping("/logout")
    public String logout() {
        // Spring Security의 로그아웃 처리를 사용
        return "redirect:/page/users/login?logout";
    }

    // 기존 로그인 URL에 대한 리다이렉트
    @GetMapping("/login")
    public String login() {
        return "user/login-form";
    }

    // 기존 홈 URL에 대한 리다이렉트
    @GetMapping("/home")
    public String home() {
        return "redirect:/page/users/home";
    }

    // 기존 사용자 관련 URL에 대한 리다이렉트
    @GetMapping("/user/form")
    public String userForm() {
        return "redirect:/page/users/form";
    }

    @GetMapping("/user/list")
    public String userList() {
        return "redirect:/page/users/list";
    }

    // 기존 회의실 예약 관련 URL에 대한 리다이렉트
    @GetMapping("/page/meeting")
    public String meeting() {
        return "redirect:/page/meetings";
    }

    @GetMapping("/meetings/reserve")
    public String reserve() {
        return "redirect:/page/rooms";
    }
}