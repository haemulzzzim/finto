// package com.haemulzzzim.fintobe.meeting_room.controller;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import
// com.haemulzzzim.fintobe.meeting_room.service.RoomTimeSlotPerformanceTest;

// /**
// * 테스트 API 컨트롤러
// * 개발 및 테스트 용도의 REST API 요청을 처리합니다.
// * 실제 서비스 배포 시에는 제거하는 것이 좋습니다.
// */
// @RestController
// @RequestMapping("/api/test")
// public class TestApiController {

// @Autowired
// private RoomTimeSlotPerformanceTest performanceTest;

// /**
// * 병렬 처리와 순차 처리의 성능 테스트를 실행합니다.
// *
// * @return 성능 테스트 실행 결과 메시지
// */
// @GetMapping("/parallel-vs-sequential")
// public String runPerformanceTest() {
// try {
// performanceTest.runTest();
// return "성능 테스트가 서버 로그에 기록되었습니다. 서버 로그를 확인하세요.";
// } catch (Exception e) {
// return "성능 테스트 실행 중 오류 발생: " + e.getMessage();
// }
// }
// }