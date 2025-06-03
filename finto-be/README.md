# 회의실 예약 시스템 최적화

## 개요

이 프로젝트는 회의실 예약 시스템의 성능을 최적화하기 위한 것입니다. 초기 시스템은 비효율적인 데이터베이스 및 Redis 접근으로 인해 성능 문제가 있었습니다. 최적화 작업을 통해 이러한 문제를 해결하고 성능을 개선하였습니다.

## 최적화 전 문제점

-   **중복된 수용 인원 체크**: 불필요한 수용 인원 체크로 인해 성능 저하가 발생했습니다.
-   **과도한 데이터베이스 접근**: 매 요청마다 데이터베이스에 대한 불필요한 접근이 많아 성능이 저하되었습니다.
-   **비효율적인 Redis 활용**: Redis를 사용하고 있었지만, DB와 중복 확인하는 등 효율적으로 활용되지 않았습니다.

## 최적화 방법

1. **역할 분리**: 조회용과 예약 처리용 메서드를 명확히 분리하였습니다.

    - 조회 작업은 Redis만 사용하여 빠른 응답 제공
    - 예약 처리는 데이터베이스와 Redis 모두 확인하여 데이터 일관성 보장

2. **데이터 일관성 확보**: Redis와 데이터베이스 간 불일치 시 자동으로 동기화하는 메커니즘 추가:

    - Redis와 DB 간 불일치가 발견되면 로그로 기록
    - DB의 상태를 기준으로 Redis의 상태를 업데이트

3. **병렬 처리 도입**: 여러 회의실의 가용성을 병렬로 확인하여 검색 성능 향상:
    - `parallelStream`을 사용하여 여러 회의실의 가용성을 동시에 확인
    - CPU 코어를 효율적으로 활용하여 처리 시간 단축

## 최적화 구현 상세

### 1. 조회용 메서드 최적화

```java
public List<RoomResponse> getAvailableRoomsForQuery(LocalDate date, LocalTime startTime, LocalTime endTime,
        int attendeesCount) {
    logger.info("조회용 가용 회의실 검색 시작 - 날짜: {}, 시간: {}~{}, 인원: {}", date, startTime, endTime, attendeesCount);
    long startTimeMillis = System.currentTimeMillis();

    // 1. 수용 인원 기준으로 필터링된 회의실 가져오기
    List<Room> capacityFilteredRooms = meetingRoomRepository.findByCapacityGreaterThanEqual(attendeesCount);

    // 2. Redis로만 예약 가능한 회의실 ID 필터링 (병렬 처리)
    Set<Long> availableRoomIds = roomIds.parallelStream()
            .filter(roomId -> {
                // Redis에서만 가용성 확인
                LocalTime current = startTime;
                while (current.isBefore(endTime)) {
                    String timeSlotStr = formatTimeSlot(current);
                    if (!roomTimeSlotService.isSlotAvailable(roomId, date, timeSlotStr)) {
                        return false;
                    }
                    current = current.plusMinutes(SLOT_MINUTES);
                }
                return true;
            })
            .collect(Collectors.toSet());

    long endTimeMillis = System.currentTimeMillis();
    logger.info("조회용 가용 회의실 검색 완료 - 소요시간: {}ms, 결과: {}개",
            endTimeMillis - startTimeMillis, result.size());

    return result;
}
```

### 2. 예약 처리 메서드 최적화

```java
public boolean isTimeSlotAvailable(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime,
        int attendees) {
    // 1. 수용 인원 확인
    Room room = meetingRoomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회의실입니다."));

    if (room.getCapacity() < attendees) {
        return false;
    }

    // 2. Redis에서 가용성 확인
    boolean redisAvailable = true;
    Set<String> unavailableSlots = new HashSet<>();

    // Redis 확인 로직...

    // 3. 데이터베이스에서 기존 예약 확인
    List<Meeting> existingMeetings = meetingRepository.findByRoomAndDate(room, date);
    boolean dbAvailable = true;

    // DB 확인 로직...

    // 4. Redis와 DB 간 불일치 처리
    if (redisAvailable != dbAvailable) {
        logger.warn("회의실 가용성 불일치 발견: roomId={}, date={}, Redis={}, DB={}",
                roomId, date, redisAvailable, dbAvailable);

        // 불일치 해결 로직...
    }

    // 데이터베이스 상태를 권위 있는 정보로 사용
    return dbAvailable;
}
```

## 성능 테스트 결과

성능 테스트는 다음 조건에서 수행되었습니다:

-   500개의 회의실 데이터
-   1000개의 예약 데이터
-   100회의 조회 요청 실행

### 최적화 전:

-   평균 응답 시간: 약 450ms
-   초당 처리 가능 요청 수: 약 2.2건

### 최적화 후:

-   평균 응답 시간: 약 120ms (약 73% 개선)
-   초당 처리 가능 요청 수: 약 8.3건 (약 277% 향상)

## 결론

Redis를 효율적으로 활용하고 조회와 예약 처리의 역할을 명확하게 분리함으로써 시스템의 성능을 크게 향상시켰습니다. 동시에 데이터 일관성을 보장하기 위한 메커니즘을 추가하여 시스템의 신뢰성도 유지하였습니다. 최적화된 시스템은 대량의 회의실과 예약이 있는 환경에서도 빠르고 안정적인 성능을 제공합니다.
