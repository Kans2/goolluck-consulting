package com.seatlock.service;

import com.seatlock.dto.LockRequest;
import com.seatlock.dto.LockResponse;
import com.seatlock.dto.SeatDTO;
import com.seatlock.exception.SeatUnavailableException;
import com.seatlock.model.SeatStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for the seat-locking workflow.
 *
 * <p>These tests spin up the full Spring context with an H2 in-memory database
 * and use multiple threads to verify that the pessimistic-locking strategy
 * prevents race conditions.</p>
 *
 * <h3>What is tested</h3>
 * <ul>
 *   <li>Two concurrent sessions trying to lock the <em>same</em> seat — only
 *       one should succeed; the other must receive a 409/SeatUnavailableException.</li>
 *   <li>Two concurrent sessions locking <em>different</em> seats — both should
 *       succeed without interference.</li>
 *   <li>High-contention scenario with 10 threads all racing for the same seat —
 *       exactly one winner.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("h2")
class SeatServiceConcurrencyTest {

    @Autowired
    private SeatService seatService;

    // ── Test 1: Two threads, same seat ───────────────────────────────────

    @Test
    @DisplayName("Only one of two concurrent lock requests for the same seat should succeed")
    void concurrentLockOnSameSeat_onlyOneSucceeds() throws Exception {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            String sessionId = UUID.randomUUID().toString();
            futures.add(executor.submit(() -> {
                try {
                    startGate.await(); // synchronise start
                    LockRequest req = new LockRequest(List.of(1L), sessionId);
                    seatService.lockSeats(req);
                    successes.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        startGate.countDown(); // fire!

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();

        assertEquals(1, successes.get(),
                "Exactly one thread should successfully lock the seat");
        assertEquals(1, conflicts.get(),
                "Exactly one thread should receive a conflict");

        // Verify the seat is LOCKED
        SeatDTO seat = seatService.getAllSeats().stream()
                .filter(s -> s.id() == 1L)
                .findFirst()
                .orElseThrow();
        assertEquals(SeatStatus.LOCKED, seat.status());
    }

    // ── Test 2: Two threads, different seats ─────────────────────────────

    @Test
    @DisplayName("Concurrent lock requests for different seats should both succeed")
    void concurrentLockOnDifferentSeats_bothSucceed() throws Exception {
        // Use seats 5 and 6 to avoid conflicts with test 1
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        // Thread A locks seat 5
        futures.add(executor.submit(() -> {
            try {
                startGate.await();
                seatService.lockSeats(new LockRequest(
                        List.of(5L), UUID.randomUUID().toString()));
                successes.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        // Thread B locks seat 6
        futures.add(executor.submit(() -> {
            try {
                startGate.await();
                seatService.lockSeats(new LockRequest(
                        List.of(6L), UUID.randomUUID().toString()));
                successes.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        startGate.countDown();

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();

        assertEquals(2, successes.get(),
                "Both threads should succeed since they lock different seats");
    }

    // ── Test 3: High contention — 10 threads, one seat ──────────────────

    @Test
    @DisplayName("10 concurrent threads racing for the same seat — exactly 1 winner")
    void highContention_exactlyOneWinner() throws Exception {
        int threadCount = 10;
        // Use seat 10 to avoid conflicts with other tests
        long targetSeatId = 10L;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            String sessionId = UUID.randomUUID().toString();
            futures.add(executor.submit(() -> {
                try {
                    startGate.await();
                    seatService.lockSeats(new LockRequest(
                            List.of(targetSeatId), sessionId));
                    successes.incrementAndGet();
                } catch (SeatUnavailableException e) {
                    conflicts.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        startGate.countDown();

        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }

        executor.shutdown();

        assertEquals(1, successes.get(),
                "Exactly one of 10 threads should win the lock");
        assertEquals(threadCount - 1, conflicts.get(),
                "All other threads should get a conflict");
    }
}
