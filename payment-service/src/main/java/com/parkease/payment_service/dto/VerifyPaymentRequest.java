package com.parkease.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for verifying a Razorpay payment after checkout.
 * Contains order ID, payment ID, and cryptographic signature for validation.
 */
@Data
public class VerifyPaymentRequest {

    @NotNull(message = "paymentId is required")
    private Long paymentId;

    @NotBlank(message = "razorpayPaymentId is required")
    private String razorpayPaymentId;

    @NotBlank(message = "razorpayOrderId is required")
    private String razorpayOrderId;

    @NotBlank(message = "razorpaySignature is required")
    private String razorpaySignature;
}
