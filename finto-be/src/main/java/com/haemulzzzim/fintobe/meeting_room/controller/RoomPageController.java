package com.haemulzzzim.fintobe.meeting_room.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.haemulzzzim.fintobe.meeting_room.service.CustomUserDetails;
import com.haemulzzzim.fintobe.meeting_room.service.RoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 회의실 페이지 컨트롤러
 */
@Controller
@RequestMapping("/page/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomPageController {

    private static final String REDIRECT_LOGIN_PATH = "redirect:/login";
    private final RoomService roomService;

    /**
     * 회의실 목록 페이지를 보여줍니다.
     */
    @GetMapping
    public String showRoomListPage(Model model, @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return REDIRECT_LOGIN_PATH;
        }
        model.addAttribute("rooms", roomService.getAllRoomsWithTimeSlots(LocalDate.now()));
        return "meeting-room/room-list";
    }
}