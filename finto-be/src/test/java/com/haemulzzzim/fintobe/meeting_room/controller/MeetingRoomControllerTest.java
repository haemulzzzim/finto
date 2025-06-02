package com.haemulzzzim.fintobe.meeting_room.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MeetingRoomControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("회의실 이름 중복이면 409 반환")
    void createRoom_duplicateName() throws Exception {
        String name = "중복회의실";
        // 첫 번째 생성
        mockMvc.perform(post("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"name\": \"" + name + "\"," +
                        "\"location\": \"1층\"," +
                        "\"capacity\": 10}"))
                .andExpect(status().isCreated());
        // 두 번째 생성(중복)
        mockMvc.perform(post("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"name\": \"" + name + "\"," +
                        "\"location\": \"2층\"," +
                        "\"capacity\": 20}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("회의실명")));
    }
}