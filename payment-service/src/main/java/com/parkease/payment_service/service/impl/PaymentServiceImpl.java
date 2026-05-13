package com.parkease.payment_service.service.impl;

import com.parkease.payment_service.client.BookingServiceClient;
import com.parkease.payment_service.dto.*;
import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.enums.PaymentMethod;
import com.parkease.payment_service.enums.PaymentStatus;
import com.parkease.payment_service.exception.BadRequestException;
import com.parkease.payment_service.exception.ResourceNotFoundException;
import com.parkease.payment_service.repository.PaymentRepository;
import com.parkease.payment_service.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingServiceClient;
    private final RazorpayClient razorpayClient;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;


    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              BookingServiceClient bookingServiceClient,
                              RazorpayClient razorpayClient,
                              @Qualifier("razorpayKeyId") String razorpayKeyId,
                              @Qualifier("razorpayKeySecret") String razorpayKeySecret) {
        this.paymentRepository = paymentRepository;
        this.bookingServiceClient = bookingServiceClient;
        this.razorpayClient = razorpayClient;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
    }

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        // Check if payment already exists for this booking
        var existing = paymentRepository.findByBookingId(request.getBookingId());
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Payment already completed for booking #" + request.getBookingId());
        }

        // Generate unique receipt number
        String receipt = "PE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", (int) (Math.random() * 10000));

        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency("INR")
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.UPI)
                .status(PaymentStatus.PENDING)
                .receiptNumber(receipt)
                .description(request.getDescription())
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment record created: {} for booking #{}", payment.getPaymentId(), request.getBookingId());
        return toResponse(payment);
    }

    /**
     * Create a Razorpay order for the given payment.
     * Returns order details that frontend uses to open Razorpay Checkout.
     */
    public RazorpayOrderResponse createRazorpayOrder(CreatePaymentRequest request) {
        // Create payment record first
        PaymentResponse paymentResponse = createPayment(request);
        Payment payment = findPayment(paymentResponse.getPaymentId());

        try {
            // Amount in paise (Razorpay expects smallest currency unit)
            long amountInPaise = Math.round(request.getAmount() * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", payment.getReceiptNumber());

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            // Store Razorpay order ID
            payment.setRazorpayOrderId(orderId);
            paymentRepository.save(payment);

            log.info("Razorpay order created: {} for payment #{}", orderId, payment.getPaymentId());

            return RazorpayOrderResponse.builder()
                    .orderId(orderId)
                    .razorpayKeyId(razorpayKeyId)
                    .amount(amountInPaise)
                    .currency("INR")
                    .paymentId(payment.getPaymentId())
                    .description(request.getDescription() != null ? request.getDescription() : "ParkEase Parking")
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("Payment gateway error: " + e.getMessage());
        }
    }

    /**
     * Verify the payment signature from Razorpay and mark as SUCCESS.
     */
    public PaymentResponse verifyPayment(VerifyPaymentRequest request) {
        Payment payment = findPayment(request.getPaymentId());

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean valid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (valid) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
                payment.setRazorpaySignature(request.getRazorpaySignature());
                payment.setTransactionId(request.getRazorpayPaymentId());
                payment.setPaidAt(LocalDateTime.now());
                payment = paymentRepository.save(payment);
                log.info("Payment #{} verified and confirmed. Razorpay ID: {}", payment.getPaymentId(), request.getRazorpayPaymentId());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new BadRequestException("Payment verification failed — invalid signature.");
            }
        } catch (RazorpayException e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.error("Razorpay verification error: {}", e.getMessage());
            throw new BadRequestException("Payment verification error: " + e.getMessage());
        }

        return toResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentById(Long paymentId) {
        return toResponse(findPayment(paymentId));
    }

    @Override
    public PaymentResponse getPaymentByBooking(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for booking #" + bookingId));
    }

    @Override
    public List<PaymentResponse> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public Double getTotalSpentByUser(Long userId) {
        return paymentRepository.getTotalSpentByUser(userId);
    }

    @Override
    public PaymentResponse confirmPayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Payment is not in PENDING state.");
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        return toResponse(payment);
    }

    @Override
    public PaymentResponse refundPayment(Long paymentId) {
        Payment payment = findPayment(paymentId);
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Can only refund SUCCESS payments.");
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);
        return toResponse(payment);
    }

    @Override
    public RevenueResponse getLotRevenue(Long lotId) {
        try {
            List<Map<String, Object>> bookings = bookingServiceClient.getBookingsByLot(lotId);
            if (bookings == null || bookings.isEmpty()) {
                return RevenueResponse.builder().lotId(lotId).totalRevenue(0.0).totalPayments(0L).build();
            }

            double totalRevenue = 0.0;
            long totalPayments = 0;
            for (Map<String, Object> booking : bookings) {
                Long bookingId = ((Number) booking.get("bookingId")).longValue();
                var payment = paymentRepository.findByBookingId(bookingId);
                if (payment.isPresent() && payment.get().getStatus() == PaymentStatus.SUCCESS) {
                    totalRevenue += payment.get().getAmount();
                    totalPayments++;
                }
            }

            return RevenueResponse.builder().lotId(lotId).totalRevenue(totalRevenue).totalPayments(totalPayments).build();
        } catch (Exception e) {
            log.error("Failed to fetch bookings for lot {}: {}", lotId, e.getMessage());
            return RevenueResponse.builder().lotId(lotId).totalRevenue(0.0).totalPayments(0L).build();
        }
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .bookingId(p.getBookingId())
                .userId(p.getUserId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentMethod(p.getPaymentMethod())
                .status(p.getStatus())
                .transactionId(p.getTransactionId())
                .receiptNumber(p.getReceiptNumber())
                .description(p.getDescription())
                .paidAt(p.getPaidAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
