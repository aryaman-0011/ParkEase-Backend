package com.parkease.booking_service.dto;

import com.parkease.booking_service.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private Long bookingId;
    private Long userId;
    private Long spotId;
    private Long lotId;
    private String lotName;
    private String spotNumber;
    private String vehiclePlate;
    private BookingStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double pricePerHour;
    private Double totalCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
