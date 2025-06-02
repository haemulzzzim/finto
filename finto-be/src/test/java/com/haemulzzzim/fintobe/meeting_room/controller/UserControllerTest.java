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
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("사원번호 중복이면 409 반환")
    void createUser_duplicateEmpno() throws Exception {
        String empno = "123456";
        // 첫 번째 생성
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"empno\": \"" + empno + "\"," +
                        "\"name\": \"홍길동\"," +
                        "\"email\": \"hong1@email.com\"}"))
                .andExpect(status().isCreated());
        // 두 번째 생성(중복)
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"empno\": \"" + empno + "\"," +
                        "\"name\": \"김철수\"," +
                        "\"email\": \"chulsoo@email.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("사원번호")));
    }

    @Test
    @DisplayName("이메일 중복이면 409 반환")
    void createUser_duplicateEmail() throws Exception {
        String email = "dup@email.com";
        // 첫 번째 생성
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"empno\": \"654321\"," +
                        "\"name\": \"홍길동\"," +
                        "\"email\": \"" + email + "\"}"))
                .andExpect(status().isCreated());
        // 두 번째 생성(중복)
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "\"empno\": \"111111\"," +
                        "\"name\": \"김철수\"," +
                        "\"email\": \"" + email + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("이메일")));
    }
}