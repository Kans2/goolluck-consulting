package com.seatlock.exception;

import com.seatlock.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralised exception handling for all REST controllers.
 *
 * <p>Each custom exception is mapped to an appropriate HTTP status code and
 * returned as a structured {@link ErrorResponse} envelope.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 409 CONFLICT — seat not available ────────────────────────────────

    @ExceptionHandler(SeatUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleSeatUnavailable(
            SeatUnavailableException ex, HttpServletRequest request) {

        log.warn("Seat conflict: {}", ex.getMessage());
        ErrorResponse body = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                ex.getUnavailableSeatIds(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ── 410 GONE — lock expired ──────────────────────────────────────────

    @ExceptionHandler(LockExpiredException.class)
    public ResponseEntity<ErrorResponse> handleLockExpired(
            LockExpiredException ex, HttpServletRequest request) {

        log.warn("Lock expired: {}", ex.getMessage());
        ErrorResponse body = new ErrorResponse(
                HttpStatus.GONE.value(),
                "Gone",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.GONE).body(body);
    }

    // ── 404 NOT FOUND — lock not found ───────────────────────────────────

    @ExceptionHandler(LockNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleLockNotFound(
            LockNotFoundException ex, HttpServletRequest request) {

        log.warn("Lock not found: {}", ex.getMessage());
        ErrorResponse body = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ── 400 BAD REQUEST — bean validation failures ───────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse body = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                details,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // ── 500 INTERNAL SERVER ERROR — catch-all ────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        ErrorResponse body = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.internalServerError().body(body);
    }
}
