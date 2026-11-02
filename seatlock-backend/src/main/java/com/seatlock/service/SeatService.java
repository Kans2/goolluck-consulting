package com.seatlock.service;

import com.seatlock.dto.*;
import com.seatlock.exception.LockExpiredException;
import com.seatlock.exception.LockNotFoundException;
import com.seatlock.exception.SeatUnavailableException;
import com.seatlock.model.*;
import com.seatlock.repository.BookingRepository;
import com.seatlock.repository.SeatLockRepository;
import com.seatlock.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Core business logic for the seat-booking workflow.
 *
 * <h3>Concurrency Model</h3>
 * <ol>
 *   <li><strong>Lock acquisition</strong> uses {@code SELECT … FOR UPDATE} via
 *       {@link SeatRepository#findAllByIdForUpdate(List)}.  This acquires
 *       exclusive row-level locks in the DB, blocking any concurrent
 *       transaction that tries to read or modify the same seat rows.</li>
 *   <li>All seat IDs are sorted before locking to avoid deadlocks when two
 *       transactions lock overlapping sets.</li>
 *   <li>A UNIQUE constraint on {@code bookings.seat_id} acts as the final
 *       safeguard against double-booking at the DB level.</li>
 * </ol>
 */
@Service
public class SeatService {

    private static final Logger log = LoggerFactory.getLogger(SeatService.class);

    /** How long a lock is valid before the scheduled task expires it. */
    private static final Duration LOCK_TTL = Duration.ofMinutes(2);

    private final SeatRepository seatRepository;
    private final SeatLockRepository seatLockRepository;
    private final BookingRepository bookingRepository;
    private final LockExpiryService lockExpiryService;

    public SeatService(SeatRepository seatRepository,
                       SeatLockRepository seatLockRepository,
                       BookingRepository bookingRepository,
                       LockExpiryService lockExpiryService) {
        this.seatRepository = seatRepository;
        this.seatLockRepository = seatLockRepository;
        this.bookingRepository = bookingRepository;
        this.lockExpiryService = lockExpiryService;
    }

    // ── GET /api/seats ───────────────────────────────────────────────────

    /**
     * Returns all 20 seats with their current status.
     * Stale locks are automatically expired first so that seats whose
     * 2-minute timer has passed are returned as AVAILABLE.
     */
    @Transactional
    public List<SeatDTO> getAllSeats() {
        lockExpiryService.expireStaleLocks();
        return seatRepository.findAll()
                .stream()
                .map(SeatDTO::from)
                .toList();
    }

    // ── POST /api/seats/lock ─────────────────────────────────────────────

    /**
     * Atomically locks the requested seats for a given session.
     *
     * <h4>Algorithm</h4>
     * <ol>
     *   <li>Sort requested IDs to avoid deadlocks.</li>
     *   <li>{@code SELECT … FOR UPDATE} — blocks concurrent writers.</li>
     *   <li>Verify all seats are {@code AVAILABLE}.</li>
     *   <li>Set status to {@code LOCKED}, create {@link SeatLock} records.</li>
     *   <li>Commit releases the row-level locks.</li>
     * </ol>
     *
     * @param request lock request containing seat IDs and session ID
     * @return lock response with TTL information
     * @throws SeatUnavailableException if any seat is not AVAILABLE
     */
    @Transactional
    public LockResponse lockSeats(LockRequest request) {
        // Expire any past-due locks first
        lockExpiryService.expireStaleLocks();

        // 1. Sort IDs to guarantee consistent lock ordering → prevents deadlocks
        List<Long> sortedIds = request.seatIds().stream().sorted().toList();

        log.info("Session {} requesting lock on seats {}", request.sessionId(), sortedIds);

        // 2. SELECT … FOR UPDATE — acquires exclusive row-level locks
        List<Seat> seats = seatRepository.findAllByIdForUpdate(sortedIds);

        // Verify all requested seats exist
        if (seats.size() != sortedIds.size()) {
            List<Long> foundIds = seats.stream().map(Seat::getId).toList();
            List<Long> missingIds = sortedIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new SeatUnavailableException(missingIds);
        }

        // 3. Check all seats are AVAILABLE
        List<Long> unavailable = seats.stream()
                .filter(s -> s.getStatus() != SeatStatus.AVAILABLE)
                .map(Seat::getId)
                .toList();

        if (!unavailable.isEmpty()) {
            log.warn("Session {} — seats {} are not available", request.sessionId(), unavailable);
            throw new SeatUnavailableException(unavailable);
        }

        // 4. Lock them — set status and create SeatLock records
        Instant now = Instant.now();
        Instant expiresAt = now.plus(LOCK_TTL);

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.LOCKED);

            SeatLock lock = new SeatLock(seat, request.sessionId(), now, expiresAt);
            seatLockRepository.save(lock);
        }

        seatRepository.saveAll(seats);

        log.info("Session {} locked seats {} until {}",
                request.sessionId(), sortedIds, expiresAt);

        // 5. Return response — row locks are released when transaction commits
        return new LockResponse(
                request.sessionId(),
                sortedIds,
                now,
                expiresAt,
                "Seats locked successfully. You have 2 minutes to confirm."
        );
    }

    // ── POST /api/seats/confirm ──────────────────────────────────────────

    /**
     * Confirms a booking for all seats locked by the given session.
     *
     * <h4>Algorithm</h4>
     * <ol>
     *   <li>Find all ACTIVE locks for the session.</li>
     *   <li>Verify none have expired.</li>
     *   <li>{@code SELECT … FOR UPDATE} on the seat rows.</li>
     *   <li>Set seats to BOOKED, locks to CONFIRMED, create Booking records.</li>
     * </ol>
     *
     * @param request confirmation request with session ID and passenger name
     * @return confirmation response
     * @throws LockNotFoundException  if no active locks exist for the session
     * @throws LockExpiredException   if the locks' TTL has elapsed
     */
    @Transactional
    public ConfirmResponse confirmBooking(ConfirmRequest request) {
        log.info("Session {} confirming booking for '{}'",
                request.sessionId(), request.passengerName());

        // 1. Find ACTIVE locks for this session
        List<SeatLock> activeLocks = seatLockRepository
                .findBySessionIdAndStatus(request.sessionId(), LockStatus.ACTIVE);

        if (activeLocks.isEmpty()) {
            throw new LockNotFoundException(request.sessionId());
        }

        // 2. Check expiry — if any lock has expired, reject the whole batch
        Instant now = Instant.now();
        boolean anyExpired = activeLocks.stream()
                .anyMatch(lock -> lock.getExpiresAt().isBefore(now));

        if (anyExpired) {
            // Mark them all as expired and release seats
            for (SeatLock lock : activeLocks) {
                lock.setStatus(LockStatus.EXPIRED);
                lock.getSeat().setStatus(SeatStatus.AVAILABLE);
            }
            seatLockRepository.saveAll(activeLocks);
            throw new LockExpiredException(request.sessionId());
        }

        // 3. SELECT … FOR UPDATE on the seat rows
        List<Long> seatIds = activeLocks.stream()
                .map(lock -> lock.getSeat().getId())
                .sorted()
                .toList();

        List<Seat> seats = seatRepository.findAllByIdForUpdate(seatIds);

        // 4. Transition: LOCKED → BOOKED, ACTIVE → CONFIRMED, create Bookings
        Instant bookedAt = Instant.now();

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.BOOKED);

            Booking booking = new Booking(seat, request.sessionId(), request.passengerName());
            booking.setBookedAt(bookedAt);
            bookingRepository.save(booking);
        }

        for (SeatLock lock : activeLocks) {
            lock.setStatus(LockStatus.CONFIRMED);
        }

        seatRepository.saveAll(seats);
        seatLockRepository.saveAll(activeLocks);

        log.info("Session {} confirmed booking for seats {} — passenger '{}'",
                request.sessionId(), seatIds, request.passengerName());

        return new ConfirmResponse(
                request.sessionId(),
                request.passengerName(),
                seatIds,
                bookedAt,
                "Booking confirmed successfully!"
        );
    }

    // ── DELETE /api/seats/lock/{sessionId} ───────────────────────────────

    /**
     * Cancels all active locks held by the given session, releasing seats
     * back to {@code AVAILABLE}.
     *
     * @param sessionId the session whose locks should be cancelled
     * @throws LockNotFoundException if no active locks exist for the session
     */
    @Transactional
    public void cancelLocks(String sessionId) {
        log.info("Session {} cancelling locks", sessionId);

        List<SeatLock> activeLocks = seatLockRepository
                .findBySessionIdAndStatus(sessionId, LockStatus.ACTIVE);

        if (activeLocks.isEmpty()) {
            throw new LockNotFoundException(sessionId);
        }

        // Acquire row locks on seats to safely reset their status
        List<Long> seatIds = activeLocks.stream()
                .map(lock -> lock.getSeat().getId())
                .sorted()
                .toList();

        List<Seat> seats = seatRepository.findAllByIdForUpdate(seatIds);

        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.AVAILABLE);
        }

        for (SeatLock lock : activeLocks) {
            lock.setStatus(LockStatus.CANCELLED);
        }

        seatRepository.saveAll(seats);
        seatLockRepository.saveAll(activeLocks);

        log.info("Session {} cancelled locks on seats {}", sessionId, seatIds);
    }

    // ── GET /api/bookings ────────────────────────────────────────────────

    /**
     * Returns all confirmed bookings.
     */
    @Transactional(readOnly = true)
    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(BookingDTO::from)
                .toList();
    }
}
