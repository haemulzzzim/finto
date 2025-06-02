package com.haemulzzzim.fintobe.meeting_room.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.haemulzzzim.fintobe.meeting_room.service.DataSynchronizer;

/**
 * 관리자 API 컨트롤러
 * 관리자 전용 REST API 요청을 처리합니다.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminApiController {

    private static final Logger logger = LoggerFactory.getLogger(AdminApiController.class);

    private final DataSynchronizer dataSynchronizer;

    @Autowired
    public AdminApiController(DataSynchronizer dataSynchronizer) {
        this.dataSynchronizer = dataSynchronizer;
    }

    /**
     * Redis와 DB 간의 회의실 예약 데이터를 수동으로 전체 재동기화합니다.
     * 이 API는 관리자만 접근할 수 있습니다.
     * 
     * @return 동기화 결과 메시지
     */
    @PostMapping("/sync/meetings")
    @PreAuthorize("hasRole('ADMIN')") // 관리자 권한 필요
    public ResponseEntity<String> syncMeetingData() {
        logger.info("관리자 요청: 회의실 예약 데이터 수동 동기화 시작");

        try {
            // 전체 재동기화 실행
            dataSynchronizer.fullResynchronization();

            return ResponseEntity.ok("회의실 예약 데이터 동기화가 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            logger.error("회의실 예약 데이터 동기화 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("데이터 동기화 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}