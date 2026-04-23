package com.parkease.payment_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RevenueResponse {
    private Long lotId;
    private Double totalRevenue;
    private Long totalPayments;
}
