package com.seatlock.service;

import com.seatlock.model.LockStatus;
import com.seatlock.model.Seat;
import com.seatlock.model.SeatLock;
import com.seatlock.model.SeatStatus;
import com.seatlock.repository.SeatLockRepository;
import com.seatlock.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Periodic background task that expires stale seat locks.
 *
 * <p>Runs every 5 seconds. Any {@code ACTIVE} lock whose {@code expiresAt}
 * is in the past is transitioned to {@code EXPIRED}, and the associated seat
 * is set back to {@code AVAILABLE} and saved to the database so that users
 * can immediately select it again.</p>
 */
@Service
public class LockExpiryService {

    private static final Logger log = LoggerFactory.getLogger(LockExpiryService.class);

    private final SeatLockRepository seatLockRepository;
    private final SeatRepository seatRepository;

    public LockExpiryService(SeatLockRepository seatLockRepository,
                             SeatRepository seatRepository) {
        this.seatLockRepository = seatLockRepository;
        this.seatRepository = seatRepository;
    }

    /**
     * Scheduled task: expire stale locks every 5 seconds.
     */
    @Scheduled(fixedRate = 5_000)
    @Transactional
    public void expireStaleLocks() {
        Instant now = Instant.now();
        List<SeatLock> expired = seatLockRepository
                .findExpiredLocks(LockStatus.ACTIVE, now);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Expiring {} stale lock(s)", expired.size());

        List<Seat> seatsToRelease = new ArrayList<>();
        for (SeatLock lock : expired) {
            lock.setStatus(LockStatus.EXPIRED);
            Seat seat = lock.getSeat();
            seat.setStatus(SeatStatus.AVAILABLE);
            seatsToRelease.add(seat);

            log.debug("Expired lock {} — seat {} (session {})",
                    lock.getId(),
                    seat.getSeatNumber(),
                    lock.getSessionId());
        }

        seatRepository.saveAll(seatsToRelease);
        seatLockRepository.saveAll(expired);

        log.info("Expired {} lock(s) — seats released back to AVAILABLE", expired.size());
    }
}
