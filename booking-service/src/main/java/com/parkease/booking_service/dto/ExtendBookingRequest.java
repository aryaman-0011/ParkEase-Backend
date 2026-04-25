package com.parkease.booking_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExtendBookingRequest {

    @NotNull(message = "newEndTime is required")
    private LocalDateTime newEndTime;
}
