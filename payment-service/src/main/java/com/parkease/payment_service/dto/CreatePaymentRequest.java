package com.parkease.payment_service.dto;

import com.parkease.payment_service.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for initiating a new payment.
 * Contains booking, user, amount, and payment method information.
 */
@Data
public class CreatePaymentRequest {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "amount is required")
    private Double amount;

    private PaymentMethod paymentMethod;

    private String description;
}
