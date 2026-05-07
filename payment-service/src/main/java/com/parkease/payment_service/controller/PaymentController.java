package com.parkease.payment_service.controller;

import com.parkease.payment_service.dto.*;
import com.parkease.payment_service.service.PaymentService;
import com.parkease.payment_service.service.impl.PaymentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// REST controller for payments — Razorpay integration, payment history, revenue reports
@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentServiceImpl paymentService;

    // Create a Razorpay order — frontend uses the returned orderId to open Razorpay checkout
    @PostMapping("/create-order")
    public ResponseEntity<RazorpayOrderResponse> createOrder(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createRazorpayOrder(request));
    }

    // Verify Razorpay payment signature after checkout completes on frontend
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }

    // Create a payment record without Razorpay (for cash payments)
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }

    // Get a single payment by its ID
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    // Get the payment associated with a specific booking
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBooking(bookingId));
    }

    // Get all payments made by a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getUserPayments(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }

    // Get total amount spent by a user across all payments
    @GetMapping("/user/{userId}/total")
    public ResponseEntity<Map<String, Object>> getTotalSpent(@PathVariable Long userId) {
        Double total = paymentService.getTotalSpentByUser(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "totalSpent", total));
    }

    // Manually confirm a pending payment (admin/manager action)
    @PutMapping("/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.confirmPayment(id));
    }

    // Refund a completed payment
    @PutMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }

    // Get revenue breakdown for a specific lot (total, today, this month)
    @GetMapping("/lot/{lotId}/revenue")
    public ResponseEntity<RevenueResponse> getLotRevenue(@PathVariable Long lotId) {
        return ResponseEntity.ok(paymentService.getLotRevenue(lotId));
    }
}
