package com.parkease.booking_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Request DTO for extending an active booking end time.
 * The backend validates the new end time against potential conflicts.
 */
@Data
public class ExtendBookingRequest {

    @NotNull(message = "newEndTime is required")
    private LocalDateTime newEndTime;
}
