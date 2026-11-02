package com.seatlock.dto;

import java.time.Instant;
import java.util.List;

/**
 * Successful response from {@code POST /api/seats/lock}.
 *
 * @param sessionId     the session that now holds the locks
 * @param lockedSeatIds IDs of the seats that were locked
 * @param lockedAt      when the lock was acquired
 * @param expiresAt     when the lock will auto-expire (lockedAt + 2 min)
 * @param message       human-readable confirmation
 */
public record LockResponse(
        String sessionId,
        List<Long> lockedSeatIds,
        Instant lockedAt,
        Instant expiresAt,
        String message
) {
}
