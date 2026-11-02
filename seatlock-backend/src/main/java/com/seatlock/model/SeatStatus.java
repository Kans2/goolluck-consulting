package com.seatlock.model;

/**
 * Lifecycle states for a {@link Seat}.
 *
 * <ul>
 *   <li>{@code AVAILABLE} — open for locking</li>
 *   <li>{@code LOCKED}    — temporarily reserved (2-minute TTL)</li>
 *   <li>{@code BOOKED}    — permanently confirmed</li>
 * </ul>
 */
public enum SeatStatus {
    AVAILABLE,
    LOCKED,
    BOOKED
}
