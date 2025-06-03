package com.haemulzzzim.fintobe.meeting_room.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RoomSlotServiceTest {
    @Autowired
    RoomSlotService roomSlotService;

    @Test
    void reserve_and_cancel_slot() {
        Long roomId = 1L;
        LocalDate date = LocalDate.now();
        String slot = "09:00";
        assertThat(roomSlotService.isSlotAvailable(roomId, date, slot)).isTrue();
        assertThat(roomSlotService.reserveSlot(roomId, date, slot)).isTrue();
        assertThat(roomSlotService.isSlotAvailable(roomId, date, slot)).isFalse();
        roomSlotService.cancelSlot(roomId, date, slot);
        assertThat(roomSlotService.isSlotAvailable(roomId, date, slot)).isTrue();
    }

    @Test
    void atomic_reserve_slot_concurrency() throws InterruptedException {
        Long roomId = 2L;
        LocalDate date = LocalDate.now();
        String slot = "10:00";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    roomSlotService.cancelSlot(roomId, date, slot); // 초기화
                    boolean reserved = roomSlotService.tryReserveSlotAtomic(roomId, date, slot);
                    if (reserved) {
                        System.out.println("예약 성공");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        Set<String> reserved = roomSlotService.getReservedSlots(roomId, date);
        assertThat(reserved).contains(slot);
        assertThat(reserved.size()).isEqualTo(1); // 단 한 번만 예약됨
    }
}