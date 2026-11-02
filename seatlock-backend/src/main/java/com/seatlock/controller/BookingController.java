package com.seatlock.controller;

import com.seatlock.dto.BookingDTO;
import com.seatlock.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for booking queries.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final SeatService seatService;

    public BookingController(SeatService seatService) {
        this.seatService = seatService;
    }

    /**
     * GET /api/bookings — list all confirmed bookings.
     */
    @GetMapping
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        return ResponseEntity.ok(seatService.getAllBookings());
    }
}
