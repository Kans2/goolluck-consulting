package com.seatlock.repository;

import com.seatlock.model.LockStatus;
import com.seatlock.model.SeatLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Data-access layer for {@link SeatLock} entities.
 */
@Repository
public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {

    /**
     * Find all locks for a session that are currently in a given status.
     */
    List<SeatLock> findBySessionIdAndStatus(String sessionId, LockStatus status);

    /**
     * Find all ACTIVE locks whose TTL has elapsed.
     * Used by the scheduled expiry task.
     */
    @Query("SELECT sl FROM SeatLock sl WHERE sl.status = :status AND sl.expiresAt < :now")
    List<SeatLock> findExpiredLocks(@Param("status") LockStatus status,
                                   @Param("now") Instant now);
}
