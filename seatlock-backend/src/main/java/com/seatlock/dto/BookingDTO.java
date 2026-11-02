package com.seatlock.dto;

import com.seatlock.model.Booking;

import java.time.Instant;

/**
 * A read-only projection of {@link Booking}.
 *
 * @param id            booking primary key
 * @param seatId        booked seat ID
 * @param seatNumber    human-readable seat label
 * @param sessionId     the session that made the booking
 * @param passengerName passenger name
 * @param bookedAt      confirmation timestamp
 */
public record BookingDTO(
        Long id,
        Long seatId,
        String seatNumber,
        String sessionId,
        String passengerName,
        Instant bookedAt
) {

    /** Factory method to convert an entity to its DTO. */
    public static BookingDTO from(Booking booking) {
        return new BookingDTO(
                booking.getId(),
                booking.getSeat().getId(),
                booking.getSeat().getSeatNumber(),
                booking.getSessionId(),
                booking.getPassengerName(),
                booking.getBookedAt()
        );
    }
}
