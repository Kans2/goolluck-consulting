package com.seatlock.exception;

import java.util.List;

/**
 * Thrown when one or more requested seats are not in {@code AVAILABLE} status.
 * Carries the list of unavailable seat IDs so the client can highlight them.
 */
public class SeatUnavailableException extends RuntimeException {

    private final List<Long> unavailableSeatIds;

    public SeatUnavailableException(List<Long> unavailableSeatIds) {
        super("The following seats are not available: " + unavailableSeatIds);
        this.unavailableSeatIds = unavailableSeatIds;
    }

    public List<Long> getUnavailableSeatIds() {
        return unavailableSeatIds;
    }
}
