package com.haemulzzzim.fintobe.meeting_room.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.haemulzzzim.fintobe.meeting_room.dto.UserDto;
import com.haemulzzzim.fintobe.meeting_room.entity.Employee;
import com.haemulzzzim.fintobe.meeting_room.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 API 컨트롤러
 * 사용자 관련 REST API 요청을 처리합니다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserApiController {
    private final UserService userService;

    /**
     * 사용자 목록 조회 API
     * 
     * @return 사용자 목록
     */
    @GetMapping
    public ResponseEntity<List<Employee>> getAllUsers() {
        List<Employee> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * 사용자 상세 조회 API
     * 
     * @param id 사용자 ID
     * @return 사용자 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        Optional<Employee> user = userService.findById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 사용자 등록 API
     * 
     * @param dto 사용자 정보
     * @return 등록 결과
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDto dto) {
        Employee saved = userService.save(dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * 사용자 수정 API
     * 
     * @param id  사용자 ID
     * @param dto 수정할 사용자 정보
     * @return 수정 결과
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserDto dto) {
        Employee updated = userService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * 사용자 삭제 API
     * 
     * @param id 사용자 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok().body("사용자가 삭제되었습니다.");
    }

    /**
     * 로그아웃 API
     * 실제 로그아웃은 Spring Security에서 처리합니다.
     * 
     * @return 로그아웃 성공 메시지
     */
    @GetMapping("/logout")
    public ResponseEntity<?> logout() {
        log.info("로그아웃 API 호출됨");
        return ResponseEntity.ok().body("로그아웃 되었습니다.");
    }
}