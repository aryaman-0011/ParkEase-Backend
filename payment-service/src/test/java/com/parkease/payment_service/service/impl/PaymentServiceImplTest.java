package com.parkease.payment_service.service.impl;

import com.parkease.payment_service.dto.CreatePaymentRequest;
import com.parkease.payment_service.dto.PaymentResponse;
import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.enums.PaymentMethod;
import com.parkease.payment_service.enums.PaymentStatus;
import com.parkease.payment_service.exception.BadRequestException;
import com.parkease.payment_service.exception.ResourceNotFoundException;
import com.parkease.payment_service.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private RazorpayClient razorpayClient;

    private PaymentServiceImpl paymentService;

    private Payment pendingPayment;
    private Payment successPayment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository, restTemplate, razorpayClient,
                "rzp_test_key", "rzp_test_secret"
        );

        LocalDateTime now = LocalDateTime.now();
        pendingPayment = Payment.builder()
                .paymentId(1L).bookingId(100L).userId(10L)
                .amount(150.0).currency("INR")
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.PENDING)
                .receiptNumber("PE-20260501-0001")
                .description("Parking fee")
                .createdAt(now).updatedAt(now)
                .build();

        successPayment = Payment.builder()
                .paymentId(2L).bookingId(200L).userId(10L)
                .amount(300.0).currency("INR")
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.SUCCESS)
                .receiptNumber("PE-20260501-0002")
                .transactionId("txn_abc123")
                .paidAt(now)
                .createdAt(now).updatedAt(now)
                .build();
    }

    private CreatePaymentRequest buildCreateRequest(Long bookingId, Long userId, Double amount,
            PaymentMethod method, String description) {
        CreatePaymentRequest req = new CreatePaymentRequest();
        req.setBookingId(bookingId);
        req.setUserId(userId);
        req.setAmount(amount);
        req.setPaymentMethod(method);
        req.setDescription(description);
        return req;
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {
        @Test
        @DisplayName("should create a PENDING payment record")
        void createSuccess() {
            CreatePaymentRequest req = buildCreateRequest(100L, 10L, 150.0,
                    PaymentMethod.UPI, "Parking fee");

            when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

            PaymentResponse response = paymentService.createPayment(req);

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(response.getAmount()).isEqualTo(150.0);
            assertThat(response.getCurrency()).isEqualTo("INR");
        }

        @Test
        @DisplayName("should throw when payment already completed for booking")
        void createDuplicate() {
            CreatePaymentRequest req = buildCreateRequest(200L, 10L, 300.0, null, null);

            when(paymentRepository.findByBookingId(200L)).thenReturn(Optional.of(successPayment));

            assertThatThrownBy(() -> paymentService.createPayment(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already completed");
        }

        @Test
        @DisplayName("should default to UPI when method is null")
        void createDefaultMethod() {
            CreatePaymentRequest req = buildCreateRequest(300L, 10L, 100.0, null, null);

            when(paymentRepository.findByBookingId(300L)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setPaymentId(3L);
                return p;
            });

            PaymentResponse response = paymentService.createPayment(req);

            assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
        }
    }

    @Nested
    @DisplayName("getPaymentById")
    class GetPaymentById {
        @Test
        @DisplayName("should return payment when found")
        void found() {
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

            PaymentResponse response = paymentService.getPaymentById(1L);

            assertThat(response.getPaymentId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw when not found")
        void notFound() {
            when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPaymentByBooking")
    class GetByBooking {
        @Test
        @DisplayName("should return payment for booking")
        void found() {
            when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(pendingPayment));

            PaymentResponse response = paymentService.getPaymentByBooking(100L);

            assertThat(response.getBookingId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("should throw when no payment for booking")
        void notFound() {
            when(paymentRepository.findByBookingId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentByBooking(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPaymentsByUser")
    class GetByUser {
        @Test
        @DisplayName("should return all user payments")
        void byUser() {
            when(paymentRepository.findByUserIdOrderByCreatedAtDesc(10L))
                    .thenReturn(List.of(pendingPayment, successPayment));

            List<PaymentResponse> result = paymentService.getPaymentsByUser(10L);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getTotalSpentByUser")
    class TotalSpent {
        @Test
        @DisplayName("should return total amount spent")
        void totalSpent() {
            when(paymentRepository.getTotalSpentByUser(10L)).thenReturn(450.0);

            assertThat(paymentService.getTotalSpentByUser(10L)).isEqualTo(450.0);
        }

        @Test
        @DisplayName("should return null when no payments")
        void noPayments() {
            when(paymentRepository.getTotalSpentByUser(999L)).thenReturn(null);

            assertThat(paymentService.getTotalSpentByUser(999L)).isNull();
        }
    }

    @Nested
    @DisplayName("confirmPayment")
    class ConfirmPayment {
        @Test
        @DisplayName("should confirm PENDING payment")
        void confirmSuccess() {
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            PaymentResponse response = paymentService.confirmPayment(1L);

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(pendingPayment.getPaidAt()).isNotNull();
        }

        @Test
        @DisplayName("should throw when not PENDING")
        void confirmNotPending() {
            when(paymentRepository.findById(2L)).thenReturn(Optional.of(successPayment));

            assertThatThrownBy(() -> paymentService.confirmPayment(2L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("PENDING");
        }
    }

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {
        @Test
        @DisplayName("should refund SUCCESS payment")
        void refundSuccess() {
            when(paymentRepository.findById(2L)).thenReturn(Optional.of(successPayment));
            when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            PaymentResponse response = paymentService.refundPayment(2L);

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("should throw when refunding non-SUCCESS payment")
        void refundNotSuccess() {
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));

            assertThatThrownBy(() -> paymentService.refundPayment(1L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("SUCCESS");
        }
    }

    @Nested
    @DisplayName("getLotRevenue")
    class GetLotRevenue {
        @Test
        @DisplayName("should return zero when no bookings")
        void noBookings() {
            when(restTemplate.getForObject(anyString(), eq(List.class))).thenReturn(null);

            var revenue = paymentService.getLotRevenue(200L);

            assertThat(revenue.getTotalRevenue()).isEqualTo(0.0);
            assertThat(revenue.getTotalPayments()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle service error gracefully")
        void serviceError() {
            when(restTemplate.getForObject(anyString(), eq(List.class)))
                    .thenThrow(new RuntimeException("Connection refused"));

            var revenue = paymentService.getLotRevenue(200L);

            assertThat(revenue.getTotalRevenue()).isEqualTo(0.0);
        }
    }
}
