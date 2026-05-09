package com.parkease.payment_service.controller;

import com.parkease.payment_service.dto.*;
import com.parkease.payment_service.enums.*;
import com.parkease.payment_service.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {
    @Mock private PaymentServiceImpl service;
    @InjectMocks private PaymentController controller;

    private PaymentResponse sample() {
        return PaymentResponse.builder().paymentId(1L).bookingId(10L).userId(5L).amount(200.0)
                .status(PaymentStatus.SUCCESS).paymentMethod(PaymentMethod.UPI)
                .paidAt(LocalDateTime.now()).build();
    }

    @Test void createOrder() {
        when(service.createRazorpayOrder(any())).thenReturn(
                RazorpayOrderResponse.builder().orderId("order_123").amount(20000L).currency("INR").build());
        assertEquals(HttpStatus.CREATED, controller.createOrder(new CreatePaymentRequest()).getStatusCode());
    }
    @Test void verifyPayment() {
        when(service.verifyPayment(any())).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.verifyPayment(new VerifyPaymentRequest()).getStatusCode());
    }
    @Test void createPayment() {
        when(service.createPayment(any())).thenReturn(sample());
        assertEquals(HttpStatus.CREATED, controller.createPayment(new CreatePaymentRequest()).getStatusCode());
    }
    @Test void getPayment() {
        when(service.getPaymentById(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.getPayment(1L).getStatusCode());
    }
    @Test void getPaymentByBooking() {
        when(service.getPaymentByBooking(10L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.getPaymentByBooking(10L).getStatusCode());
    }
    @Test void getUserPayments() {
        when(service.getPaymentsByUser(5L)).thenReturn(List.of(sample()));
        assertEquals(1, controller.getUserPayments(5L).getBody().size());
    }
    @Test void getTotalSpent() {
        when(service.getTotalSpentByUser(5L)).thenReturn(500.0);
        assertEquals(500.0, controller.getTotalSpent(5L).getBody().get("totalSpent"));
    }
    @Test void confirmPayment() {
        when(service.confirmPayment(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.confirmPayment(1L).getStatusCode());
    }
    @Test void refundPayment() {
        when(service.refundPayment(1L)).thenReturn(sample());
        assertEquals(HttpStatus.OK, controller.refundPayment(1L).getStatusCode());
    }
    @Test void getLotRevenue() {
        when(service.getLotRevenue(2L)).thenReturn(
                RevenueResponse.builder().lotId(2L).totalRevenue(5000.0).totalPayments(10L).build());
        assertEquals(HttpStatus.OK, controller.getLotRevenue(2L).getStatusCode());
    }
}
