// package com.haemulzzzim.fintobe.meeting_room.service;

// import java.time.LocalDate;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Random;
// import java.util.Set;
// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.stream.Collectors;
// import java.util.stream.IntStream;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.data.redis.core.Cursor;
// import org.springframework.data.redis.core.RedisTemplate;
// import org.springframework.data.redis.core.ScanOptions;
// import org.springframework.stereotype.Component;

// /**
//  * Redis 순차 처리와 병렬 처리의 성능을 비교하는 테스트 컴포넌트
//  * 실제 서비스 코드는 아니며, 테스트 및 검증 목적으로만 사용됩니다.
//  */
// @Component
// public class RoomTimeSlotPerformanceTest {

//     private static final Logger logger = LoggerFactory.getLogger(RoomTimeSlotPerformanceTest.class);

//     @Autowired
//     private RedisTemplate<String, String> redisTemplate;

//     // 테스트용 샘플 데이터 크기
//     private static final int[] TEST_DATA_SIZES = { 10, 100, 1000, 5000 };

//     // 테스트 실행 횟수 (평균 계산용)
//     private static final int TEST_RUNS = 5;

//     // 검사할 날짜 범위 (1~30일 전)
//     private static final int DATE_RANGE = 30;

//     /**
//      * 애플리케이션 시작 후 자동 실행 (실제 운영 환경에서는 비활성화하는 것이 좋습니다)
//      * 활성화하려면 아래의 PostConstruct 주석을 해제하세요.
//      */
//     // @PostConstruct
//     public void runPerformanceTest() {
//         logger.info("============ Redis 병렬/순차 처리 성능 테스트 시작 ============");

//         // 테스트 데이터 생성 (실제 서비스 데이터에 영향 없도록 별도 키 사용)
//         setupTestData();

//         // 각 데이터 크기별로 테스트 실행
//         for (int size : TEST_DATA_SIZES) {
//             runTestForSize(size);
//         }

//         // 테스트 데이터 정리
//         cleanupTestData();

//         logger.info("============ Redis 병렬/순차 처리 성능 테스트 완료 ============");
//     }

//     /**
//      * 특정 데이터 크기에 대한 테스트 실행
//      */
//     private void runTestForSize(int dataSize) {
//         logger.info("데이터 크기: {} 항목에 대한 테스트 실행", dataSize);

//         double avgSequentialTime = 0;
//         double avgParallelTime = 0;

//         for (int run = 0; run < TEST_RUNS; run++) {
//             // 순차 처리 테스트
//             long sequentialStart = System.currentTimeMillis();
//             int sequentialResult = testSequentialProcessing(dataSize);
//             long sequentialTime = System.currentTimeMillis() - sequentialStart;

//             // 병렬 처리 테스트
//             long parallelStart = System.currentTimeMillis();
//             int parallelResult = testParallelProcessing(dataSize);
//             long parallelTime = System.currentTimeMillis() - parallelStart;

//             avgSequentialTime += sequentialTime;
//             avgParallelTime += parallelTime;

//             logger.info("실행 #{}: 순차 처리={} ms, 병렬 처리={} ms, 결과 동일={}",
//                     run + 1, sequentialTime, parallelTime, sequentialResult == parallelResult);
//         }

//         avgSequentialTime /= TEST_RUNS;
//         avgParallelTime /= TEST_RUNS;

//         double speedup = avgSequentialTime / avgParallelTime;

//         logger.info("데이터 크기 {} 항목 결과: 순차={} ms, 병렬={} ms, 속도 향상={}배",
//                 dataSize, avgSequentialTime, avgParallelTime, speedup);

//         if (speedup > 1.0) {
//             logger.info("병렬 처리가 {}배 더 빠릅니다.", String.format("%.2f", speedup));
//         } else {
//             logger.info("순차 처리가 {}배 더 빠릅니다.", String.format("%.2f", 1 / speedup));
//         }
//     }

//     /**
//      * 순차 처리 테스트
//      */
//     private int testSequentialProcessing(int dataSize) {
//         AtomicInteger totalProcessed = new AtomicInteger(0);

//         LocalDate yesterday = LocalDate.now().minusDays(1);

//         for (int i = 1; i <= DATE_RANGE; i++) {
//             LocalDate targetDate = yesterday.minusDays(i);
//             String dateStr = targetDate.toString().replace("-", "");
//             String pattern = "test:perf:" + dataSize + ":slots:" + dateStr + ":*";

//             // SCAN 사용
//             Set<String> keys = scanRedisKeys(pattern);

//             if (keys != null && !keys.isEmpty()) {
//                 // 순차 처리로 삭제 (실제 삭제는 하지 않고 카운트만)
//                 keys.forEach(key -> {
//                     // 실제 삭제 대신 카운트
//                     totalProcessed.incrementAndGet();

//                     // 약간의 CPU 작업 시뮬레이션 (실제 작업 모방)
//                     String value = redisTemplate.opsForValue().get(key);
//                     if (value != null) {
//                         int hash = value.hashCode();
//                         hash = hash * 31 + key.hashCode();
//                     }
//                 });
//             }
//         }

//         return totalProcessed.get();
//     }

//     /**
//      * 병렬 처리 테스트
//      */
//     private int testParallelProcessing(int dataSize) {
//         AtomicInteger totalProcessed = new AtomicInteger(0);

//         LocalDate yesterday = LocalDate.now().minusDays(1);

//         List<LocalDate> datesToProcess = IntStream.rangeClosed(1, DATE_RANGE)
//                 .mapToObj(yesterday::minusDays)
//                 .collect(Collectors.toList());

//         // 병렬 스트림으로 각 날짜 처리
//         datesToProcess.parallelStream().forEach(targetDate -> {
//             String dateStr = targetDate.toString().replace("-", "");
//             String pattern = "test:perf:" + dataSize + ":slots:" + dateStr + ":*";

//             // SCAN 사용
//             Set<String> keys = scanRedisKeys(pattern);

//             if (keys != null && !keys.isEmpty()) {
//                 // 병렬 처리로 키 처리 (실제 삭제는 하지 않고 카운트만)
//                 keys.parallelStream().forEach(key -> {
//                     // 실제 삭제 대신 카운트
//                     totalProcessed.incrementAndGet();

//                     // 약간의 CPU 작업 시뮬레이션 (실제 작업 모방)
//                     String value = redisTemplate.opsForValue().get(key);
//                     if (value != null) {
//                         int hash = value.hashCode();
//                         hash = hash * 31 + key.hashCode();
//                     }
//                 });
//             }
//         });

//         return totalProcessed.get();
//     }

//     /**
//      * Redis SCAN 명령으로 키를 검색
//      */
//     private Set<String> scanRedisKeys(String pattern) {
//         Set<String> keys = new HashSet<>();
//         try (Cursor<String> cursor = redisTemplate.scan(ScanOptions.scanOptions()
//                 .match(pattern)
//                 .count(100)
//                 .build())) {

//             while (cursor.hasNext()) {
//                 keys.add(cursor.next());
//             }
//         } catch (Exception e) {
//             logger.warn("Redis SCAN 실행 중 오류 (패턴: {}): {}", pattern, e.getMessage());
//         }
//         return keys;
//     }

//     /**
//      * 테스트용 데이터 생성
//      */
//     private void setupTestData() {
//         logger.info("테스트 데이터 생성 중...");

//         // 오늘 날짜 기준
//         LocalDate today = LocalDate.now();
//         Random random = new Random();

//         // 각 데이터 크기별로 데이터 생성
//         for (int size : TEST_DATA_SIZES) {
//             int roomCount = Math.min(10, size / 10); // 회의실 수 (최대 10개)
//             if (roomCount < 1)
//                 roomCount = 1;

//             int entriesPerDate = size / (DATE_RANGE * roomCount);
//             if (entriesPerDate < 1)
//                 entriesPerDate = 1;

//             // 날짜별 데이터 생성
//             for (int days = 1; days <= DATE_RANGE; days++) {
//                 LocalDate date = today.minusDays(days);
//                 String dateStr = date.toString().replace("-", "");

//                 // 회의실별 데이터 생성
//                 for (int room = 1; room <= roomCount; room++) {
//                     // 각 시간대별 슬롯 생성
//                     for (int entry = 0; entry < entriesPerDate; entry++) {
//                         int hour = 7 + random.nextInt(14); // 7시 ~ 20시
//                         int minute = random.nextInt(2) * 30; // 0분 또는 30분
//                         String time = String.format("%02d:%02d", hour, minute);

//                         String key = String.format("test:perf:%d:slots:%s:%d:%s",
//                                 size, dateStr, room, time);
//                         redisTemplate.opsForValue().set(key, "TEST-VALUE");
//                     }
//                 }
//             }
//         }

//         logger.info("테스트 데이터 생성 완료");
//     }

//     /**
//      * 테스트 데이터 정리
//      */
//     private void cleanupTestData() {
//         logger.info("테스트 데이터 정리 중...");

//         for (int size : TEST_DATA_SIZES) {
//             String pattern = "test:perf:" + size + ":*";
//             Set<String> keys = redisTemplate.keys(pattern);
//             if (keys != null && !keys.isEmpty()) {
//                 redisTemplate.delete(keys);
//                 logger.info("크기 {} 테스트 데이터 {} 항목 삭제됨", size, keys.size());
//             }
//         }

//         logger.info("테스트 데이터 정리 완료");
//     }

//     /**
//      * 이 메서드를 직접 호출하여 테스트를 실행할 수 있습니다
//      */
//     public void runTest() {
//         runPerformanceTest();
//     }
// }