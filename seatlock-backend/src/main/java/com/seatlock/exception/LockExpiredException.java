package com.seatlock.exception;

/**
 * Thrown when a confirmation is attempted but the session's locks have
 * already expired (TTL elapsed without confirmation).
 */
public class LockExpiredException extends RuntimeException {

    public LockExpiredException(String sessionId) {
        super("Locks for session " + sessionId + " have expired. Please select your seats again.");
    }
}
