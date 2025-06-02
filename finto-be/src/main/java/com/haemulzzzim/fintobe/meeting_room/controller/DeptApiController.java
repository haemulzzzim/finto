package com.haemulzzzim.fintobe.meeting_room.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.haemulzzzim.fintobe.meeting_room.entity.Dept;
import com.haemulzzzim.fintobe.meeting_room.service.DeptService;

import lombok.RequiredArgsConstructor;

/**
 * 부서 API 컨트롤러
 * 부서 관련 REST API 요청을 처리합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/depts")
public class DeptApiController {
    private final DeptService deptService;

    /**
     * 새로운 부서를 생성합니다.
     */
    @PostMapping
    public ResponseEntity<?> createDept(@RequestBody Dept dept) {
        Dept saved = deptService.createDept(dept);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}