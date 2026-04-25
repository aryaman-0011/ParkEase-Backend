package com.parkease.payment_service.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for revenue summary data.
 * Returns total spent amount for a user or total revenue for admin.
 */
@Data
@Builder
public class RevenueResponse {
    private Long lotId;
    private Double totalRevenue;
    private Long totalPayments;
}
