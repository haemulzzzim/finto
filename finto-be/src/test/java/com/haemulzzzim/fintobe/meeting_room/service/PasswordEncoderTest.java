package com.haemulzzzim.fintobe.meeting_room.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderTest {
    @Test
    void printEncodedPassword() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "rkdgywls159";
        String encoded = encoder.encode(rawPassword);
        System.out.println("원본: " + rawPassword);
        System.out.println("암호화: " + encoded);

    }

    @Test
    void test() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean matches = encoder.matches("rkdgywls159",
                "$2a$10$fEsbopb3bsqZFFR/i9Fqe.efloS6u8sdNoKK.s.iOc4pDtdNXl7YC");
        System.out.println("matches: " + matches);
    }
}