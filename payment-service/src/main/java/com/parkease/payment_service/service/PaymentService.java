package com.parkease.payment_service.service;

import com.parkease.payment_service.dto.CreatePaymentRequest;
import com.parkease.payment_service.dto.PaymentResponse;
import com.parkease.payment_service.dto.RevenueResponse;
import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse getPaymentById(Long paymentId);

    PaymentResponse getPaymentByBooking(Long bookingId);

    List<PaymentResponse> getPaymentsByUser(Long userId);

    Double getTotalSpentByUser(Long userId);

    PaymentResponse confirmPayment(Long paymentId);

    PaymentResponse refundPayment(Long paymentId);

    RevenueResponse getLotRevenue(Long lotId);
}
