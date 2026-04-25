package com.parkease.payment_service.dto;

import com.parkease.payment_service.enums.PaymentMethod;
import com.parkease.payment_service.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Response DTO representing a payment record returned to the client.
 * Includes amount, method, status, transaction ID, and receipt number.
 */
@Data
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long bookingId;
    private Long userId;
    private Double amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String transactionId;
    private String receiptNumber;
    private String description;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
