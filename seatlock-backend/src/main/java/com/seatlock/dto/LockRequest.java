package com.seatlock.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request body for {@code POST /api/seats/lock}.
 *
 * @param seatIds   IDs of the seats to lock (must not be empty)
 * @param sessionId client-generated UUID identifying this browser session
 */
public record LockRequest(
        @NotEmpty(message = "seatIds must not be empty")
        List<Long> seatIds,

        @NotBlank(message = "sessionId is required")
        String sessionId
) {
}
