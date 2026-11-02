package com.seatlock.model;

/**
 * Lifecycle states for a {@link SeatLock}.
 *
 * <ul>
 *   <li>{@code ACTIVE}    — lock is held and the user has not yet confirmed</li>
 *   <li>{@code CONFIRMED} — user confirmed; booking was created</li>
 *   <li>{@code EXPIRED}   — TTL elapsed without confirmation</li>
 *   <li>{@code CANCELLED} — user explicitly released the lock</li>
 * </ul>
 */
public enum LockStatus {
    ACTIVE,
    CONFIRMED,
    EXPIRED,
    CANCELLED
}
