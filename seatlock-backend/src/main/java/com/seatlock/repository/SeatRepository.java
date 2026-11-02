package com.seatlock.repository;

import com.seatlock.model.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Data-access layer for {@link Seat} entities.
 *
 * <p>The {@link #findAllByIdForUpdate(List)} method is the centrepiece of
 * the concurrency strategy: it executes {@code SELECT … FOR UPDATE},
 * acquiring exclusive row-level locks that block any concurrent transaction
 * from reading or modifying the same rows until the current transaction
 * commits or rolls back.</p>
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Pessimistic-write lock on the given seat IDs.
     *
     * <p>Translates to {@code SELECT … FROM seats WHERE id IN (…) FOR UPDATE}.
     * The IDs are sorted by the DB engine, which helps avoid deadlocks when
     * two transactions lock overlapping sets of seats.</p>
     *
     * @param ids seat primary keys to lock
     * @return the locked seat entities (in DB-determined order)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :ids ORDER BY s.id")
    List<Seat> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}
