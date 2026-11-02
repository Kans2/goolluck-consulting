package com.seatlock.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Represents a single seat in the venue.
 *
 * <p>The {@code status} field tracks the seat lifecycle:
 * {@code AVAILABLE → LOCKED → BOOKED}.  Pessimistic row-level locks
 * (SELECT … FOR UPDATE) protect concurrent mutations; the {@code version}
 * column is kept as an optimistic-locking fallback for non-critical reads.</p>
 */
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_number", nullable = false, unique = true, length = 10)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;

    /**
     * Optimistic-lock version — incremented automatically by JPA on every
     * update.  Serves as a secondary defence even though the primary
     * concurrency strategy is pessimistic locking.
     */
    @Version
    private Long version;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // ── lifecycle callbacks ──────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── constructors ─────────────────────────────────────────────────────

    public Seat() {
    }

    public Seat(String seatNumber) {
        this.seatNumber = seatNumber;
        this.status = SeatStatus.AVAILABLE;
    }

    // ── getters / setters ────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
