package com.haemulzzzim.fintobe.meeting_room.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomTimeSlotService {

    private static final Logger logger = LoggerFactory.getLogger(RoomTimeSlotService.class);
    private final RedisTemplate<String, String> redisTemplate;
    private static final int START_HOUR = 7;
    private static final int END_HOUR = 21;
    private static final int SLOT_MINUTES = 30;

    // Redis 연결 실패 시 메모리에 저장할 백업용 맵
    private final ConcurrentHashMap<String, Set<String>> inMemoryReservedSlots = new ConcurrentHashMap<>();

    // Redis 연결 상태를 추적
    private final AtomicBoolean redisAvailable = new AtomicBoolean(false);

    /**
     * 서비스 시작 시 Redis 연결 상태 확인
     */
    @PostConstruct
    public void init() {
        checkRedisConnection();
    }

    /**
     * Redis 연결 상태를 주기적으로 확인 (1분마다)
     * 연결이 복구되면 메모리에 있는 데이터를 Redis로 마이그레이션
     */
    @Scheduled(fixedRate = 60000)
    public void checkAndMigrateToRedis() {
        boolean currentStatus = checkRedisConnection();

        // Redis 연결이 복구된 경우 데이터 마이그레이션
        if (currentStatus && !redisAvailable.getAndSet(true)) {
            migrateMemoryDataToRedis();
        }
    }

    /**
     * 매일 자정에 실행되는 작업으로 과거 예약 데이터 정리
     * 어제 날짜까지의 예약 데이터를 삭제합니다.
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    public void cleanupPastReservations() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        logger.info("과거 예약 데이터 정리 시작, 삭제 기준 날짜: {}", yesterday);

        if (!ensureRedisConnection()) {
            logger.warn("Redis 연결 실패, 과거 예약 데이터 정리 작업을 수행할 수 없습니다.");
            return;
        }

        // 옵션 1: 정의된 기간 내의 모든 날짜에 대해 처리
        cleanupOldReservationData(yesterday);

        // 옵션 2: 어제 날짜의 데이터만 삭제 (별도로 처리하여 로깅)
        cleanupYesterdayReservationData(yesterday);
    }

    /**
     * Redis 연결 상태 확인 및 보장
     * 
     * @return Redis 연결 가능 여부
     */
    private boolean ensureRedisConnection() {
        if (!redisAvailable.get()) {
            return checkRedisConnection();
        }
        return true;
    }

    /**
     * 이전 기간(31일 전 ~ 2일 전)의 예약 데이터 정리
     * 
     * @param yesterday 기준일(어제)
     */
    private void cleanupOldReservationData(LocalDate yesterday) {
        try {
            // 사용 가능한 SCAN 커맨드 기반 접근법 (대규모 Redis 인스턴스에 더 적합)
            cleanupUsingRedisScan(yesterday);
        } catch (Exception e) {
            logger.error("이전 기간 예약 데이터 정리 중 오류 발생: {}", e.getMessage(), e);

            // 대체 방법: keys 패턴 사용 (소규모 Redis 인스턴스에 적합, 프로덕션에서는 주의 필요)
            cleanupUsingKeysPattern(yesterday);
        }
    }

    /**
     * Redis SCAN 명령을 사용한 예약 데이터 정리 (병렬 처리 적용)
     * 
     * @param yesterday 기준일(어제)
     */
    private void cleanupUsingRedisScan(LocalDate yesterday) {
        // 날짜 범위 생성 (31일 전 ~ 2일 전)
        List<LocalDate> datesToProcess = IntStream.rangeClosed(1, 30)
                .mapToObj(yesterday::minusDays)
                .toList();

        // 병렬 스트림으로 각 날짜 처리
        datesToProcess.parallelStream().forEach(targetDate -> {
            String datePattern = targetDate.toString().replace("-", "");
            String pattern = "room:*:slots:" + datePattern;

            try {
                // SCAN 커맨드를 사용하여 키 탐색
                Set<String> keysToDelete = scanRedisKeys(pattern);
                if (!keysToDelete.isEmpty()) {
                    deleteKeysAndLog(keysToDelete, targetDate);
                }
            } catch (Exception e) {
                logger.warn("날짜 {} 처리 중 Redis SCAN 오류: {}", targetDate, e.getMessage());
            }
        });
    }

    /**
     * Redis SCAN 명령으로 키를 검색
     * 
     * @param pattern 검색 패턴
     * @return 검색된 키 집합
     */
    private Set<String> scanRedisKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        try {
            // 효율적인 SCAN 기반 구현으로 Redis 서버 차단 방지
            keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> keySet = new HashSet<>();
                ScanOptions options = ScanOptions.scanOptions()
                        .match(pattern)
                        .count(100) // 한 번에 가져올 항목 수
                        .build();

                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        keySet.add(new String(cursor.next(), "UTF-8"));
                    }
                } catch (Exception e) {
                    logger.error("Redis SCAN 커서 처리 중 오류: {}", e.getMessage(), e);
                }

                return keySet;
            });

            if (keys == null) {
                keys = new HashSet<>();
            }

            logger.debug("SCAN으로 검색된 키 개수: {}, 패턴: {}", keys.size(), pattern);
        } catch (Exception e) {
            logger.error("Redis SCAN 명령 실행 중 오류 (패턴: {}): {}", pattern, e.getMessage());
            keys = new HashSet<>(); // 오류 발생 시 빈 집합 반환
        }
        return keys;
    }

    /**
     * Redis KEYS 패턴을 사용한 예약 데이터 정리 (fallback 방법, 병렬 처리 적용)
     * 
     * @param yesterday 기준일(어제)
     */
    private void cleanupUsingKeysPattern(LocalDate yesterday) {
        // 날짜 범위 생성 (31일 전 ~ 2일 전)
        List<LocalDate> datesToProcess = IntStream.rangeClosed(1, 30)
                .mapToObj(yesterday::minusDays)
                .toList();

        // 병렬 스트림으로 각 날짜 처리
        datesToProcess.parallelStream().forEach(targetDate -> {
            try {
                String dateStr = targetDate.toString().replace("-", "");
                String pattern = "room:*:slots:" + dateStr;

                // 경고와 함께 KEYS 패턴 사용
                logger.warn("Redis SCAN 실패로 인해 KEYS 패턴 검색 사용 중: {}", pattern);
                Set<String> keys = redisTemplate.keys(pattern);

                if (!keys.isEmpty()) {
                    deleteKeysAndLog(keys, targetDate);
                }
            } catch (Exception e) {
                logger.error("날짜 {} 처리 중 오류: {}", targetDate, e.getMessage());
            }
        });
    }

    /**
     * 키 삭제 및 로깅 처리
     * 참고: Redis의 DEL 명령은 이미 O(N) 시간 복잡도로,
     * N이 큰 경우 Redis 서버 블로킹이 발생할 수 있으므로
     * 일정 크기로 배치 처리하는 것이 안전합니다.
     * 
     * @param keys 삭제할 키 컬렉션
     * @param date 관련 날짜
     */
    private void deleteKeysAndLog(Collection<String> keys, LocalDate date) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        final int BATCH_SIZE = 100; // 한 번에 삭제할 최대 키 수

        if (keys.size() <= BATCH_SIZE) {
            // 키가 적은 경우 한 번에 삭제
            redisTemplate.delete(keys);
            logger.info("삭제된 과거 예약 데이터 - 날짜: {}, 키 개수: {}", date, keys.size());
        } else {
            // 키가 많은 경우 배치 처리
            List<List<String>> batches = new ArrayList<>();
            List<String> keysList = new ArrayList<>(keys);

            for (int i = 0; i < keysList.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, keysList.size());
                batches.add(keysList.subList(i, endIndex));
            }

            // 배치 병렬 처리
            AtomicInteger totalDeleted = new AtomicInteger(0);
            batches.parallelStream().forEach(batch -> {
                int deleted = redisTemplate.delete(batch).intValue();
                totalDeleted.addAndGet(deleted);
            });

            logger.info("배치 처리로 삭제된 과거 예약 데이터 - 날짜: {}, 키 개수: {}", date, totalDeleted.get());
        }
    }

    /**
     * 어제 날짜의 예약 데이터 정리
     * 
     * @param yesterday 어제 날짜
     */
    private void cleanupYesterdayReservationData(LocalDate yesterday) {
        try {
            String yesterdayStr = yesterday.toString().replace("-", "");
            String pattern = "room:*:slots:" + yesterdayStr;

            // 어제 날짜는 중요하므로 KEYS 패턴을 직접 사용하되,
            // 삭제 작업은 배치 처리로 수행
            Set<String> yesterdayKeys = redisTemplate.keys(pattern);
            deleteKeysAndLog(yesterdayKeys, yesterday);
        } catch (Exception e) {
            logger.error("어제 날짜({})의 예약 데이터 정리 중 오류 발생: {}", yesterday, e.getMessage(), e);
        }
    }

    /**
     * Redis 연결 상태 확인
     * 
     * @return Redis 연결 가능 여부
     */
    private boolean checkRedisConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            boolean wasDown = !redisAvailable.getAndSet(true);
            if (wasDown) {
                logger.info("Redis 연결이 복구되었습니다.");
            }
            return true;
        } catch (Exception e) {
            boolean wasUp = redisAvailable.getAndSet(false);
            if (wasUp) {
                logger.warn("Redis 연결이 끊겼습니다: {}", e.getMessage());
            } else {
                logger.debug("Redis 연결 여전히 실패 상태: {}", e.getMessage());
            }
            return false;
        }
    }

    /**
     * 메모리에 있는 데이터를 Redis로 마이그레이션
     * Redis 연결이 복구되었을 때 메모리에 임시 저장된 예약 데이터를 Redis로 이전합니다.
     * 기존 Redis 데이터와 병합하여 데이터 일관성을 유지합니다.
     */
    private synchronized void migrateMemoryDataToRedis() {
        logger.info("Redis 연결이 복구되었습니다. 메모리 데이터를 Redis로 마이그레이션합니다.");

        if (inMemoryReservedSlots.isEmpty()) {
            return;
        }

        for (var entry : inMemoryReservedSlots.entrySet()) {
            try {
                String key = entry.getKey();
                Set<String> memorySlots = entry.getValue();

                if (memorySlots == null || memorySlots.isEmpty()) {
                    continue;
                }

                // 현재 Redis에 있는 데이터와 병합하기 위해 기존 데이터 조회
                Set<String> existingSlots = redisTemplate.opsForSet().members(key);

                // 트랜잭션으로 모든 슬롯 추가
                redisTemplate.execute(new SessionCallback<Object>() {
                    @Override
                    public Object execute(RedisOperations operations) throws DataAccessException {
                        operations.multi();

                        if (existingSlots != null && !existingSlots.isEmpty()) {
                            // 기존 데이터와 메모리 데이터를 병합
                            for (String slot : memorySlots) {
                                if (!existingSlots.contains(slot)) {
                                    // 기존 Redis에 없는 슬롯만 추가
                                    operations.opsForSet().add(key, slot);
                                }
                            }
                        } else {
                            // Redis에 데이터가 없으면 메모리 데이터를 모두 추가
                            for (String slot : memorySlots) {
                                operations.opsForSet().add(key, slot);
                            }
                        }

                        return operations.exec();
                    }
                });
                logger.debug("Redis에 병합된 예약 슬롯 [{}]: 메모리={}, Redis={}, 최종={}",
                        key, memorySlots.size(),
                        (existingSlots != null ? existingSlots.size() : 0),
                        redisTemplate.opsForSet().size(key));
            } catch (Exception e) {
                logger.error("Redis 마이그레이션 중 오류 발생 (reserved slots): {}", e.getMessage());
            }
        }
        logger.info("총 {}개의 예약 슬롯 키를 Redis로 마이그레이션했습니다.", inMemoryReservedSlots.size());
        inMemoryReservedSlots.clear();
    }

    /**
     * 하나의 시간 슬롯을 예약합니다.
     *
     * @param roomId 회의실 ID
     * @param date   날짜
     * @param time   시간 (HH:mm 형식)
     * @return 예약 성공 여부
     */
    public boolean reserveTimeSlot(Long roomId, LocalDate date, String time) {
        String key = getSlotKey(roomId, date);
        try {
            Long result = redisTemplate.opsForSet().add(key, time);
            boolean added = result != null && result > 0;
            redisAvailable.set(true);
            return added;
        } catch (RedisConnectionFailureException e) {
            redisAvailable.set(false);
            logger.warn("Redis 연결 실패, 메모리 내 저장으로 대체: {}", e.getMessage());
            synchronized (inMemoryReservedSlots) {
                Set<String> slots = inMemoryReservedSlots.computeIfAbsent(key, k -> new HashSet<>());
                return slots.add(time);
            }
        }
    }

    /**
     * 하나의 시간 슬롯 예약을 취소합니다.
     *
     * @param roomId 회의실 ID
     * @param date   날짜
     * @param time   시간 (HH:mm 형식)
     */
    public void freeTimeSlot(Long roomId, LocalDate date, String time) {
        String key = getSlotKey(roomId, date);
        try {
            redisTemplate.opsForSet().remove(key, time);
            redisAvailable.set(true);
        } catch (RedisConnectionFailureException e) {
            redisAvailable.set(false);
            logger.warn("Redis 연결 실패, 메모리 내 저장소에서 삭제: {}", e.getMessage());
            synchronized (inMemoryReservedSlots) {
                Set<String> slots = inMemoryReservedSlots.get(key);
                if (slots != null) {
                    slots.remove(time);
                }
            }
        }
    }

    /**
     * 예약된 슬롯 전체 조회
     */
    public Set<String> getReservedSlots(Long roomId, LocalDate date) {
        String key = getSlotKey(roomId, date);
        try {
            Set<String> redisResult = redisTemplate.opsForSet().members(key);
            redisAvailable.set(true);
            logger.debug("Redis에서 조회된 예약 슬롯 - 키: {}, 결과: {}", key, redisResult);
            return redisResult != null ? redisResult : new HashSet<>();
        } catch (RedisConnectionFailureException e) {
            redisAvailable.set(false);
            logger.warn("Redis 연결 실패, 메모리 내 저장소에서 조회: {}", e.getMessage());
            synchronized (inMemoryReservedSlots) {
                Set<String> memoryResult = inMemoryReservedSlots.getOrDefault(key, new HashSet<>());
                logger.debug("메모리에서 조회된 예약 슬롯 - 키: {}, 결과: {}", key, memoryResult);
                return memoryResult;
            }
        }
    }

    /**
     * 예약 가능 여부 체크 (동시성 안전)
     */
    public boolean isSlotAvailable(Long roomId, LocalDate date, String time) {
        logger.debug("슬롯 가용성 확인 - 회의실: {}, 날짜: {}, 시간: {}", roomId, date, time);
        String key = getSlotKey(roomId, date);

        try {
            boolean isReserved = Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, time));
            redisAvailable.set(true);
            logger.debug("Redis 슬롯 가용성 확인 결과 - 키: {}, 시간: {}, 예약됨: {}, 가용함: {}",
                    key, time, isReserved, !isReserved);
            return !isReserved; // 예약되지 않았으면 가용함
        } catch (RedisConnectionFailureException e) {
            redisAvailable.set(false);
            logger.warn("Redis 연결 실패, 메모리 내 저장소에서 확인: {}", e.getMessage());
            synchronized (inMemoryReservedSlots) {
                Set<String> reservedSlots = inMemoryReservedSlots.get(key);
                boolean isReserved = reservedSlots != null && reservedSlots.contains(time);
                logger.debug("메모리 슬롯 가용성 확인 결과 - 키: {}, 시간: {}, 예약됨: {}, 가용함: {}",
                        key, time, isReserved, !isReserved);
                return !isReserved;
            }
        }
    }

    /**
     * 원자적 예약 (동시성 안전, 트랜잭션)
     * 이 메서드는 동시 예약 시도를 처리하기 위해 Redis 트랜잭션을 사용합니다.
     */
    public boolean tryReserveSlotAtomic(Long roomId, LocalDate date, String time) {
        String key = getSlotKey(roomId, date);
        try {
            boolean result = redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                public Boolean execute(RedisOperations operations) throws DataAccessException {
                    @SuppressWarnings("unchecked")
                    RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                    ops.watch(key);
                    if (Boolean.TRUE.equals(ops.opsForSet().isMember(key, time))) {
                        ops.unwatch();
                        return false;
                    }
                    ops.multi();
                    ops.opsForSet().add(key, time);
                    return ops.exec() != null;
                }
            });
            redisAvailable.set(true);
            return result;
        } catch (RedisConnectionFailureException e) {
            redisAvailable.set(false);
            logger.warn("Redis 연결 실패, 메모리 내 저장으로 대체: {}", e.getMessage());
            synchronized (inMemoryReservedSlots) {
                Set<String> slots = inMemoryReservedSlots.computeIfAbsent(key, k -> new HashSet<>());
                if (slots.contains(time)) {
                    return false;
                }
                return slots.add(time);
            }
        }
    }

    /**
     * 특정 시간대의 모든 슬롯 예약 (시작~종료)
     * 연속된 시간대를 한 번에 예약할 때 사용
     */
    public boolean reserveTimeRange(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        try {
            if (!redisAvailable.get()) {
                checkRedisConnection();
            }

            // 모든 슬롯이 사용 가능한지 먼저 확인
            if (!areAllSlotsAvailable(roomId, date, startTime, endTime)) {
                return false;
            }

            // 모두 가능하면 트랜잭션으로 예약
            String key = getSlotKey(roomId, date);
            return redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                public Boolean execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    LocalTime current = startTime;
                    while (current.isBefore(endTime)) {
                        String timeStr = formatTimeSlot(current);
                        operations.opsForSet().add(key, timeStr);
                        current = current.plusMinutes(SLOT_MINUTES);
                    }
                    return operations.exec() != null;
                }
            });
        } catch (RedisConnectionFailureException e) {
            redisAvailable.set(false);
            logger.warn("Redis 연결 실패, 메모리 내 저장으로 대체: {}", e.getMessage());

            synchronized (inMemoryReservedSlots) {
                // 모든 슬롯이 사용 가능한지 먼저 확인
                if (!areAllSlotsAvailable(roomId, date, startTime, endTime)) {
                    return false;
                }

                // 모두 가능하면 예약
                String key = getSlotKey(roomId, date);
                Set<String> slots = inMemoryReservedSlots.computeIfAbsent(key, k -> new HashSet<>());
                LocalTime current = startTime;
                while (current.isBefore(endTime)) {
                    String timeStr = formatTimeSlot(current);
                    slots.add(timeStr);
                    current = current.plusMinutes(SLOT_MINUTES);
                }
                return true;
            }
        }
    }

    /**
     * 모든 슬롯이 가용한지 확인
     */
    private boolean areAllSlotsAvailable(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        LocalTime current = startTime;
        while (current.isBefore(endTime)) {
            String timeStr = formatTimeSlot(current);
            if (!isSlotAvailable(roomId, date, timeStr)) {
                return false; // 하나라도 불가능하면 전체 실패
            }
            current = current.plusMinutes(SLOT_MINUTES);
        }
        return true;
    }

    /**
     * 특정 시간대의 모든 슬롯 예약 취소 (시작~종료)
     */
    public void cancelTimeRange(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        try {
            if (!redisAvailable.get()) {
                checkRedisConnection();
            }

            String key = getSlotKey(roomId, date);
            redisTemplate.execute(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    LocalTime current = startTime;
                    while (current.isBefore(endTime)) {
                        String timeStr = formatTimeSlot(current);
                        operations.opsForSet().remove(key, timeStr);
                        current = current.plusMinutes(SLOT_MINUTES);
                    }
                    return operations.exec();
                }
            });
        } catch (RedisConnectionFailureException e) {
            redisAvailable.set(false);
            logger.warn("Redis 연결 실패, 메모리 내 저장소에서 삭제: {}", e.getMessage());

            synchronized (inMemoryReservedSlots) {
                String key = getSlotKey(roomId, date);
                Set<String> slots = inMemoryReservedSlots.get(key);
                if (slots != null) {
                    LocalTime current = startTime;
                    while (current.isBefore(endTime)) {
                        String timeStr = formatTimeSlot(current);
                        slots.remove(timeStr);
                        current = current.plusMinutes(SLOT_MINUTES);
                    }
                }
            }
        }
    }

    /**
     * 슬롯 키 생성 (room:ID:slots:YYYYMMDD 형식)
     */
    private String getSlotKey(Long roomId, LocalDate date) {
        return "room:" + roomId + ":slots:" + date.toString().replace("-", "");
    }

    /**
     * 시간 형식 포맷팅 (HH:mm)
     */
    private String formatTimeSlot(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    /**
     * 특정 시간대에 예약 가능한 회의실 ID 목록을 병렬로 처리하여 반환합니다.
     * 
     * @param date       날짜
     * @param startTime  시작 시간
     * @param endTime    종료 시간
     * @param allRoomIds 모든 회의실 ID 목록
     * @return 예약 가능한 회의실 ID Set
     */
    public Set<Long> getAvailableRoomIdsParallel(LocalDate date, LocalTime startTime, LocalTime endTime,
            List<Long> allRoomIds) {
        // 결과를 저장할 set (초기값은 모든 회의실)
        Set<Long> availableRoomIds = new HashSet<>(allRoomIds);

        // 각 시간 슬롯에 대해 검사
        LocalTime current = startTime;
        while (current.isBefore(endTime)) {
            final String timeSlotStr = formatTimeSlot(current);

            // 이미 제외된 회의실은 검사하지 않도록 복사본 사용
            Set<Long> roomsToCheck = new HashSet<>(availableRoomIds);

            // 병렬 스트림으로 처리
            Set<Long> unavailableRooms = roomsToCheck.parallelStream()
                    .filter(roomId -> !isSlotAvailable(roomId, date, timeSlotStr))
                    .collect(Collectors.toSet());

            availableRoomIds.removeAll(unavailableRooms);
            current = current.plusMinutes(SLOT_MINUTES);
        }

        return availableRoomIds;
    }

    /**
     * 지정된 날짜 이후의 모든 미래 시간 슬롯 데이터를 Redis에서 초기화합니다.
     * 전체 재동기화 시 사용됩니다.
     * 
     * @param fromDate 이 날짜부터 미래의 모든 데이터를 초기화
     */
    public void clearFutureTimeSlots(LocalDate fromDate) {
        logger.info("Redis 미래 예약 데이터 초기화 시작: {} 이후", fromDate);

        try {
            // 키 패턴 - 모든 회의실 예약 데이터
            String pattern = "room:*:slots:*";

            // SCAN 명령을 사용하여 키를 안전하게 검색
            Set<String> allKeys = scanRedisKeys(pattern);

            if (allKeys.isEmpty()) {
                logger.info("초기화할 미래 예약 데이터가 없습니다.");
                return;
            }

            logger.debug("검색된 총 키 개수: {}", allKeys.size());

            // 날짜 기반 필터링 - 특정 날짜 이후의 키만 필터링
            List<String> keysToDelete = allKeys.stream()
                    .filter(key -> {
                        try {
                            // 키 형식: room:{roomId}:slots:{yyyyMMdd}
                            String[] parts = key.split(":");
                            if (parts.length >= 4) {
                                String dateStr = parts[3];
                                // yyyyMMdd 형식을 LocalDate로 변환
                                if (dateStr.length() == 8) {
                                    int year = Integer.parseInt(dateStr.substring(0, 4));
                                    int month = Integer.parseInt(dateStr.substring(4, 6));
                                    int day = Integer.parseInt(dateStr.substring(6, 8));
                                    LocalDate keyDate = LocalDate.of(year, month, day);
                                    // fromDate 이후의 데이터만 선택
                                    return !keyDate.isBefore(fromDate);
                                }
                            }
                            return false;
                        } catch (Exception e) {
                            logger.warn("키 날짜 파싱 실패: {}, 원인: {}", key, e.getMessage());
                            return false;
                        }
                    })
                    .toList();

            if (keysToDelete.isEmpty()) {
                logger.info("삭제할 미래 예약 데이터가 없습니다.");
                return;
            }

            logger.info("삭제 대상 키 개수: {}", keysToDelete.size());

            // 배치 처리로 키 삭제 (Redis 부하 방지)
            deleteKeysAndLog(keysToDelete, fromDate);

            logger.info("Redis 미래 예약 데이터 초기화 완료");
        } catch (Exception e) {
            logger.error("Redis 미래 예약 데이터 초기화 중 오류: {}", e.getMessage(), e);
            throw e;
        }
    }
}