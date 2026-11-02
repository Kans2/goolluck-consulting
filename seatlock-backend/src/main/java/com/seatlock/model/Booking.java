package com.seatlock.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A confirmed booking — the permanent record that a seat has been reserved.
 *
 * <p>The UNIQUE constraint on {@code seat_id} is the ultimate database-level
 * safeguard against double-booking: even if application logic has a bug, the
 * DB will reject a duplicate insert.</p>
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** One booking per seat — enforced by a DB UNIQUE constraint. */
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "seat_id", nullable = false, unique = true)
    private Seat seat;

    /** The session that originally locked the seat. */
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "passenger_name", nullable = false, length = 255)
    private String passengerName;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt;

    // ── lifecycle callbacks ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        if (this.bookedAt == null) {
            this.bookedAt = Instant.now();
        }
    }

    // ── constructors ─────────────────────────────────────────────────────

    public Booking() {
    }

    public Booking(Seat seat, String sessionId, String passengerName) {
        this.seat = seat;
        this.sessionId = sessionId;
        this.passengerName = passengerName;
        this.bookedAt = Instant.now();
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

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public Instant getBookedAt() {
        return bookedAt;
    }

    public void setBookedAt(Instant bookedAt) {
        this.bookedAt = bookedAt;
    }
}
