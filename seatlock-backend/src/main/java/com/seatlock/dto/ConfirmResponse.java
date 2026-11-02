package com.seatlock.dto;

import java.time.Instant;
import java.util.List;

/**
 * Successful response from {@code POST /api/seats/confirm}.
 *
 * @param sessionId     the session whose booking was confirmed
 * @param passengerName the passenger name on the booking
 * @param bookedSeatIds IDs of the booked seats
 * @param bookedAt      timestamp of confirmation
 * @param message       human-readable confirmation
 */
public record ConfirmResponse(
        String sessionId,
        String passengerName,
        List<Long> bookedSeatIds,
        Instant bookedAt,
        String message
) {
}
