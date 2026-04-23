package com.parkease.payment_service.controller;

import com.parkease.payment_service.dto.*;
import com.parkease.payment_service.service.PaymentService;
import com.parkease.payment_service.service.impl.PaymentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentServiceImpl paymentService;

    /** Create a Razorpay order — frontend uses this to open checkout */
    @PostMapping("/create-order")
    public ResponseEntity<RazorpayOrderResponse> createOrder(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createRazorpayOrder(request));
    }

    /** Verify Razorpay payment after checkout completes */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }

    /** Legacy: create payment record without Razorpay */
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createPayment(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBooking(bookingId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponse>> getUserPayments(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }

    @GetMapping("/user/{userId}/total")
    public ResponseEntity<Map<String, Object>> getTotalSpent(@PathVariable Long userId) {
        Double total = paymentService.getTotalSpentByUser(userId);
        return ResponseEntity.ok(Map.of("userId", userId, "totalSpent", total));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<PaymentResponse> confirmPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.confirmPayment(id));
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }

    @GetMapping("/lot/{lotId}/revenue")
    public ResponseEntity<RevenueResponse> getLotRevenue(@PathVariable Long lotId) {
        return ResponseEntity.ok(paymentService.getLotRevenue(lotId));
    }
}
