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
class MeetingControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("회의 생성 시 gcId 중복이면 409 반환")
    void createMeeting_duplicateGcId() throws Exception {
        // given: 이미 존재하는 gcId로 회의 생성
        String gcId = "DUPLICATE_GCID_1234567890123456";
        // 첫 번째 생성
        mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"title\": \"테스트회의\"," +
                        "\"date\": \"2024-06-01\"," +
                        "\"minutePeriod\": \"30\"," +
                        "\"startTime\": \"10:00:00\"," +
                        "\"endTime\": \"11:00:00\"," +
                        "\"gcId\": \"" + gcId + "\"," +
                        "\"roomId\": 1," +
                        "\"hostEmpSeq\": 1" +
                        "}"))
                .andExpect(status().isCreated());
        // 두 번째 생성(중복)
        mockMvc.perform(post("/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"title\": \"테스트회의2\"," +
                        "\"date\": \"2024-06-01\"," +
                        "\"minutePeriod\": \"30\"," +
                        "\"startTime\": \"12:00:00\"," +
                        "\"endTime\": \"13:00:00\"," +
                        "\"gcId\": \"" + gcId + "\"," +
                        "\"roomId\": 1," +
                        "\"hostEmpSeq\": 1" +
                        "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Google Calendar ID")));
    }
}