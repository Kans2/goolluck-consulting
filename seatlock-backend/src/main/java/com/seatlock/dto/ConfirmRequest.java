package com.seatlock.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/seats/confirm}.
 *
 * @param sessionId     the session whose locks should be confirmed
 * @param passengerName name to record on the booking
 */
public record ConfirmRequest(
        @NotBlank(message = "sessionId is required")
        String sessionId,

        @NotBlank(message = "passengerName is required")
        String passengerName
) {
}
