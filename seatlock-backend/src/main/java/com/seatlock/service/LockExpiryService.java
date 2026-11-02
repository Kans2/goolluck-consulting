package com.seatlock.service;

import com.seatlock.model.LockStatus;
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
import java.util.List;

/**
 * Periodic background task that expires stale seat locks.
 *
 * <p>Runs every 30 seconds.  Any {@code ACTIVE} lock whose {@code expiresAt}
 * is in the past is transitioned to {@code EXPIRED}, and the associated seat
 * is set back to {@code AVAILABLE} so that other users can select it.</p>
 *
 * <p>This ensures that seats are never permanently stuck in {@code LOCKED}
 * status if a user abandons the page without confirming or cancelling.</p>
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
     * Scheduled task: expire stale locks every 30 seconds.
     *
     * <p>Uses a dedicated transaction so that the cleanup is atomic and does
     * not interfere with user-initiated transactions.</p>
     */
    @Scheduled(fixedRate = 30_000)
    @Transactional
    public void expireStaleLocks() {
        Instant now = Instant.now();
        List<SeatLock> expired = seatLockRepository
                .findExpiredLocks(LockStatus.ACTIVE, now);

        if (expired.isEmpty()) {
            return; // nothing to do — keep log clean
        }

        log.info("Expiring {} stale lock(s)", expired.size());

        for (SeatLock lock : expired) {
            lock.setStatus(LockStatus.EXPIRED);
            lock.getSeat().setStatus(SeatStatus.AVAILABLE);

            log.debug("Expired lock {} — seat {} (session {})",
                    lock.getId(),
                    lock.getSeat().getSeatNumber(),
                    lock.getSessionId());
        }

        seatLockRepository.saveAll(expired);

        log.info("Expired {} lock(s) — seats released back to AVAILABLE", expired.size());
    }
}
