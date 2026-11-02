package com.seatlock.exception;

/**
 * Thrown when no active locks are found for the given session ID.
 */
public class LockNotFoundException extends RuntimeException {

    public LockNotFoundException(String sessionId) {
        super("No active locks found for session: " + sessionId);
    }
}
