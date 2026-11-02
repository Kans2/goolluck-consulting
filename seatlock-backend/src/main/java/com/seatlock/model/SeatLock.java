package com.seatlock.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A temporary 2-minute reservation that ties a session to one or more seats.
 *
 * <p>One {@code SeatLock} row is created per seat per locking request.
 * The {@link LockExpiryService} background task periodically transitions
 * {@code ACTIVE} locks whose {@code expiresAt} has passed to {@code EXPIRED},
 * releasing the associated seats back to {@code AVAILABLE}.</p>
 */
@Entity
@Table(name = "seat_locks")
public class SeatLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The seat that is being held. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    /** Client-generated UUID that groups all seats in one locking request. */
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LockStatus status = LockStatus.ACTIVE;

    // ── constructors ─────────────────────────────────────────────────────

    public SeatLock() {
    }

    public SeatLock(Seat seat, String sessionId, Instant lockedAt, Instant expiresAt) {
        this.seat = seat;
        this.sessionId = sessionId;
        this.lockedAt = lockedAt;
        this.expiresAt = expiresAt;
        this.status = LockStatus.ACTIVE;
    }

    // ── getters / setters ────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LockStatus getStatus() {
        return status;
    }

    public void setStatus(LockStatus status) {
        this.status = status;
    }
}
