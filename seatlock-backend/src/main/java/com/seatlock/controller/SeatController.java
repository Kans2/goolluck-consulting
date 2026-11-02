package com.seatlock.controller;

import com.seatlock.dto.*;
import com.seatlock.service.SeatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for seat operations: listing, locking, confirming, and cancelling.
 */
@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private static final Logger log = LoggerFactory.getLogger(SeatController.class);

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    /**
     * GET /api/seats — return all 20 seats with their current status.
     */
    @GetMapping
    public ResponseEntity<List<SeatDTO>> getAllSeats() {
        return ResponseEntity.ok(seatService.getAllSeats());
    }

    /**
     * POST /api/seats/lock — lock selected seats for a session (2-min TTL).
     *
     * <p>Uses pessimistic locking internally to prevent race conditions.</p>
     *
     * @param request JSON body with seatIds and sessionId
     * @return 200 with lock info, or 409 if any seat is unavailable
     */
    @PostMapping("/lock")
    public ResponseEntity<LockResponse> lockSeats(@Valid @RequestBody LockRequest request) {
        log.info("POST /api/seats/lock — session={}, seats={}",
                request.sessionId(), request.seatIds());
        LockResponse response = seatService.lockSeats(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/seats/confirm — confirm booking (simulated payment).
     *
     * @param request JSON body with sessionId and passengerName
     * @return 200 with booking confirmation, 404 if no locks, or 410 if expired
     */
    @PostMapping("/confirm")
    public ResponseEntity<ConfirmResponse> confirmBooking(
            @Valid @RequestBody ConfirmRequest request) {
        log.info("POST /api/seats/confirm — session={}, passenger='{}'",
                request.sessionId(), request.passengerName());
        ConfirmResponse response = seatService.confirmBooking(request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/seats/lock/{sessionId} — cancel reservation, release seats.
     *
     * @param sessionId the session whose locks to cancel
     * @return 204 No Content on success
     */
    @DeleteMapping("/lock/{sessionId}")
    public ResponseEntity<Void> cancelLocks(@PathVariable String sessionId) {
        log.info("DELETE /api/seats/lock/{}", sessionId);
        seatService.cancelLocks(sessionId);
        return ResponseEntity.noContent().build();
    }
}
