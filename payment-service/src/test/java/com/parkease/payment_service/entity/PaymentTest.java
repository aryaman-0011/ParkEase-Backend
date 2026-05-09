package com.parkease.payment_service.entity;

import com.parkease.payment_service.enums.PaymentMethod;
import com.parkease.payment_service.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {
    @Test void builderAndGetters() {
        var p = Payment.builder().paymentId(1L).bookingId(10L).userId(5L)
                .amount(200.0).currency("INR").paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.SUCCESS).transactionId("txn_123")
                .razorpayOrderId("ord_1").razorpayPaymentId("pay_1").build();
        assertEquals(1L, p.getPaymentId());
        assertEquals(200.0, p.getAmount());
        assertEquals(PaymentStatus.SUCCESS, p.getStatus());
        assertEquals(PaymentMethod.UPI, p.getPaymentMethod());
        assertEquals("txn_123", p.getTransactionId());
    }
    @Test void prePersist() {
        var p = new Payment();
        p.onCreate();
        assertNotNull(p.getCreatedAt());
    }
    @Test void preUpdate() {
        var p = new Payment();
        p.onUpdate();
        assertNotNull(p.getUpdatedAt());
    }
    @Test void setters() {
        var p = new Payment();
        p.setAmount(500.0);
        p.setStatus(PaymentStatus.REFUNDED);
        p.setCurrency("USD");
        p.setDescription("Test payment");
        assertEquals(500.0, p.getAmount());
        assertEquals(PaymentStatus.REFUNDED, p.getStatus());
    }
}
