package com.parkease.payment_service.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for a created Razorpay order.
 * Contains order ID, amount, currency, and Razorpay key for frontend checkout.
 */
@Data
@Builder
public class RazorpayOrderResponse {
    private String orderId;
    private String razorpayKeyId;
    private Long amount;
    private String currency;
    private Long paymentId;
    private String description;
    private String prefillName;
    private String prefillEmail;
}
