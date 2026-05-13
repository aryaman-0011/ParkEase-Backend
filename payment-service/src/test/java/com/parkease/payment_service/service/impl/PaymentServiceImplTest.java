package com.parkease.payment_service.service.impl;

import com.parkease.payment_service.client.BookingServiceClient;
import com.parkease.payment_service.dto.CreatePaymentRequest;
import com.parkease.payment_service.dto.PaymentResponse;
import com.parkease.payment_service.dto.VerifyPaymentRequest;
import com.parkease.payment_service.entity.Payment;
import com.parkease.payment_service.enums.PaymentMethod;
import com.parkease.payment_service.enums.PaymentStatus;
import com.parkease.payment_service.exception.BadRequestException;
import com.parkease.payment_service.exception.ResourceNotFoundException;
import com.parkease.payment_service.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;

    @Mock private BookingServiceClient bookingServiceClient;
    @Mock private RazorpayClient razorpayClient;
    @Mock private OrderClient orderClient;

    private PaymentServiceImpl paymentService;

    private Payment pendingPayment;
    private Payment successPayment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository, bookingServiceClient, razorpayClient,
                "rzp_test_key", "rzp_test_secret"
        );
        razorpayClient.orders = orderClient;

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

    private VerifyPaymentRequest buildVerifyRequest() {
        VerifyPaymentRequest req = new VerifyPaymentRequest();
        req.setPaymentId(1L);
        req.setRazorpayOrderId("order_123");
        req.setRazorpayPaymentId("pay_123");
        req.setRazorpaySignature("signature");
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
    @DisplayName("createRazorpayOrder")
    class CreateRazorpayOrder {
        @Test
        @DisplayName("should create Razorpay order and store order id")
        void createOrderSuccess() throws RazorpayException {
            CreatePaymentRequest req = buildCreateRequest(100L, 10L, 150.0,
                    PaymentMethod.CARD, "Parking fee");
            Order order = new Order(new JSONObject().put("id", "order_123"));

            when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                if (payment.getPaymentId() == null) {
                    payment.setPaymentId(1L);
                }
                return payment;
            });
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));
            when(orderClient.create(any(JSONObject.class))).thenReturn(order);

            var response = paymentService.createRazorpayOrder(req);

            assertThat(response.getOrderId()).isEqualTo("order_123");
            assertThat(response.getRazorpayKeyId()).isEqualTo("rzp_test_key");
            assertThat(response.getAmount()).isEqualTo(15000L);
            assertThat(response.getPaymentId()).isEqualTo(1L);
            assertThat(response.getDescription()).isEqualTo("Parking fee");
            assertThat(pendingPayment.getRazorpayOrderId()).isEqualTo("order_123");
            verify(paymentRepository, atLeast(2)).save(any(Payment.class));
        }

        @Test
        @DisplayName("should mark payment failed when Razorpay order creation fails")
        void createOrderGatewayFailure() throws RazorpayException {
            CreatePaymentRequest req = buildCreateRequest(100L, 10L, 150.0,
                    PaymentMethod.UPI, null);

            when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
                Payment payment = invocation.getArgument(0);
                if (payment.getPaymentId() == null) {
                    payment.setPaymentId(1L);
                }
                return payment;
            });
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));
            when(orderClient.create(any(JSONObject.class))).thenThrow(new RazorpayException("gateway down"));

            assertThatThrownBy(() -> paymentService.createRazorpayOrder(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Payment gateway error");

            assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(paymentRepository, atLeast(2)).save(any(Payment.class));
        }
    }

    @Nested
    @DisplayName("verifyPayment")
    class VerifyPayment {
        @Test
        @DisplayName("should return existing success payment without re-verifying")
        void alreadySuccess() {
            VerifyPaymentRequest req = buildVerifyRequest();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(successPayment));

            PaymentResponse response = paymentService.verifyPayment(req);

            assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("should mark payment successful for valid Razorpay signature")
        void validSignature() throws RazorpayException {
            VerifyPaymentRequest req = buildVerifyRequest();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
                utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), eq("rzp_test_secret")))
                        .thenReturn(true);

                PaymentResponse response = paymentService.verifyPayment(req);

                assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
                assertThat(response.getTransactionId()).isEqualTo("pay_123");
                assertThat(pendingPayment.getPaidAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("should mark payment failed for invalid signature")
        void invalidSignature() throws RazorpayException {
            VerifyPaymentRequest req = buildVerifyRequest();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
                utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), eq("rzp_test_secret")))
                        .thenReturn(false);

                assertThatThrownBy(() -> paymentService.verifyPayment(req))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("invalid signature");
            }

            assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("should mark payment failed when Razorpay verification errors")
        void verificationGatewayError() throws RazorpayException {
            VerifyPaymentRequest req = buildVerifyRequest();
            when(paymentRepository.findById(1L)).thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            try (MockedStatic<Utils> utils = mockStatic(Utils.class)) {
                utils.when(() -> Utils.verifyPaymentSignature(any(JSONObject.class), eq("rzp_test_secret")))
                        .thenThrow(new RazorpayException("bad signature payload"));

                assertThatThrownBy(() -> paymentService.verifyPayment(req))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("Payment verification error");
            }

            assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
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
        @DisplayName("should sum successful payments for lot bookings")
        void sumsSuccessfulPayments() {
            when(bookingServiceClient.getBookingsByLot(200L)).thenReturn(List.of(
                    Map.of("bookingId", 100L),
                    Map.of("bookingId", 200L),
                    Map.of("bookingId", 300L)
            ));
            when(paymentRepository.findByBookingId(100L)).thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.findByBookingId(200L)).thenReturn(Optional.of(successPayment));
            when(paymentRepository.findByBookingId(300L)).thenReturn(Optional.empty());

            var revenue = paymentService.getLotRevenue(200L);

            assertThat(revenue.getLotId()).isEqualTo(200L);
            assertThat(revenue.getTotalRevenue()).isEqualTo(300.0);
            assertThat(revenue.getTotalPayments()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should return zero when no bookings")
        void noBookings() {
            when(bookingServiceClient.getBookingsByLot(anyLong())).thenReturn(null);

            var revenue = paymentService.getLotRevenue(200L);

            assertThat(revenue.getTotalRevenue()).isEqualTo(0.0);
            assertThat(revenue.getTotalPayments()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle service error gracefully")
        void serviceError() {
            when(bookingServiceClient.getBookingsByLot(anyLong()))
                    .thenThrow(new RuntimeException("Connection refused"));

            var revenue = paymentService.getLotRevenue(200L);

            assertThat(revenue.getTotalRevenue()).isEqualTo(0.0);
        }
    }
}
