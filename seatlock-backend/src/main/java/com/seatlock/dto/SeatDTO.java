package com.seatlock.dto;

import com.seatlock.model.Seat;
import com.seatlock.model.SeatStatus;

import java.time.Instant;

/**
 * A read-only projection of {@link Seat} — never expose the entity directly.
 *
 * @param id         seat primary key
 * @param seatNumber human-readable seat label (e.g. "A1")
 * @param status     current lifecycle state
 * @param updatedAt  last modification timestamp
 */
public record SeatDTO(
        Long id,
        String seatNumber,
        SeatStatus status,
        Instant updatedAt
) {

    /** Factory method to convert an entity to its DTO. */
    public static SeatDTO from(Seat seat) {
        return new SeatDTO(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getStatus(),
                seat.getUpdatedAt()
        );
    }
}
