package com.seatlock.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope returned by {@link com.seatlock.exception.GlobalExceptionHandler}.
 *
 * @param status           HTTP status code
 * @param error            short error label (e.g. "Conflict")
 * @param message          human-readable detail message
 * @param unavailableSeatIds optional list of seat IDs that caused a conflict (may be null)
 * @param timestamp        when the error occurred
 * @param path             the request URI that triggered the error
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        List<Long> unavailableSeatIds,
        Instant timestamp,
        String path
) {

    /** Convenience constructor without seat IDs. */
    public ErrorResponse(int status, String error, String message, String path) {
        this(status, error, message, null, Instant.now(), path);
    }

    /** Convenience constructor with seat IDs. */
    public ErrorResponse(int status, String error, String message,
                         List<Long> unavailableSeatIds, String path) {
        this(status, error, message, unavailableSeatIds, Instant.now(), path);
    }
}
