package com.mediflow.payment.service;

import com.mediflow.audit.service.AuditLogService;
import com.mediflow.medicine.exception.PaymentNotFoundException;
import com.mediflow.order.repository.OrderRepository;
import com.mediflow.payment.dto.PaymentRequestDto;
import com.mediflow.payment.dto.PaymentResponseDto;
import com.mediflow.payment.entity.Payment;
import com.mediflow.payment.entity.PaymentStatus;
import com.mediflow.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PaymentService paymentService;


    @Test
    void markPaymentSuccess_shouldChangePendingPaymentToSuccess() {

        // WHY: allows a payment to become SUCCESS only from PENDING state.
        Payment payment = new Payment();

        payment.setId(5L);
        payment.setOrderId(1L);
        payment.setUserEmail("pulak@gmail.com");
        payment.setStatus(PaymentStatus.PENDING);

        // WHY: fake payment when the service searches for payment ID 5.
        when(paymentRepository.findById(5L))
                .thenReturn(Optional.of(payment));

        // WHY: same payment object after save().
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(payment);

        // WHY: We call the actual business method that we want to test.
        PaymentResponseDto response =
                paymentService.markPaymentSuccess(5L);

        // WHY: SUCCESS status.
        assertEquals(
                PaymentStatus.SUCCESS,
                response.getStatus()
        );

        // WHY: A successful payment should receive a transaction ID.
        assertNotNull(response.getTransactionId());

        // WHY: the updated payment.
        verify(paymentRepository).save(payment);

        // WHY: the payment status change is traceable.
        verify(auditLogService).log(
                eq("pulak@gmail.com"),
                eq("PAYMENT_SUCCESS"),
                eq("PAYMENT"),
                eq(5L),
                eq("Payment marked as successful")
        );
    }
    @Test
    void markPaymentSuccess_shouldThrowException_whenPaymentIsNotPending() {

        // WHY:
        // We create a SUCCESS payment because a final payment status
        // must not be changed to SUCCESS again.
        Payment payment = new Payment();

        payment.setId(5L);
        payment.setOrderId(1L);
        payment.setUserEmail("pulak@gmail.com");
        payment.setStatus(PaymentStatus.SUCCESS);

        // WHY:
        // The service needs to find the payment before checking
        // its current status. Mockito provides the fake payment.
        when(paymentRepository.findById(5L))
                .thenReturn(Optional.of(payment));

        // WHY:
        // ASSERT:
        // The business rule says only PENDING payments can become SUCCESS.
        // Therefore IllegalArgumentException must be thrown.
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> paymentService.markPaymentSuccess(5L)
                );

        // WHY:
        // We verify that the exception contains the expected
        // business validation message.
        assertEquals(
                "Only PENDING payment can be marked as SUCCESS",
                exception.getMessage()
        );

        // WHY:
        // Since the payment status is invalid, no database update
        // should happen.
        verify(paymentRepository, never())
                .save(any(Payment.class));

        // WHY:
        // Since the payment was not successfully processed,
        // no SUCCESS audit log should be created.
        verify(auditLogService, never())
                .log(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }
    @Test
    void markPaymentSuccess_shouldThrowException_whenPaymentNotFound() {

        // WHY: Mock repository to simulate a missing payment.
        when(paymentRepository.findById(999L))
                .thenReturn(Optional.empty());

        // WHY: Verify that missing payment throws the expected exception.
        PaymentNotFoundException exception =
                assertThrows(
                        PaymentNotFoundException.class,
                        () -> paymentService.markPaymentSuccess(999L)
                );

        // WHY: Verify that the exception contains the expected message.
        assertEquals(
                "Payment not found with id: 999",
                exception.getMessage()
        );

        // WHY: Verify that no database update happens.
        verify(paymentRepository, never())
                .save(any(Payment.class));

        // WHY: Verify that no audit log is created.
        verify(auditLogService, never())
                .log(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void markPaymentFailed_shouldChangePendingPaymentToFailed() {

        // WHY: Create a PENDING payment for the failure scenario.
        Payment payment = new Payment();

        payment.setId(6L);
        payment.setOrderId(2L);
        payment.setUserEmail("pulak@gmail.com");
        payment.setStatus(PaymentStatus.PENDING);

        // WHY: Mock repository to return the payment.
        when(paymentRepository.findById(6L))
                .thenReturn(Optional.of(payment));

        // WHY: Mock save operation without using the real database.
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(payment);

        // WHY: Call the actual service method.
        PaymentResponseDto response =
                paymentService.markPaymentFailed(6L);

        // WHY: Verify that payment status became FAILED.
        assertEquals(
                PaymentStatus.FAILED,
                response.getStatus()
        );

        // WHY: Verify that the payment was saved.
        verify(paymentRepository).save(payment);

        // WHY: Verify that the failure was recorded in audit logs.
        verify(auditLogService).log(
                eq("pulak@gmail.com"),
                eq("PAYMENT_FAILED"),
                eq("PAYMENT"),
                eq(6L),
                eq("Payment marked as failed")
        );
    }
    @Test
    void markPaymentFailed_shouldThrowException_whenPaymentIsNotPending() {

        // WHY: Create a SUCCESS payment for invalid failure attempt.
        Payment payment = new Payment();

        payment.setId(7L);
        payment.setOrderId(3L);
        payment.setUserEmail("pulak@gmail.com");
        payment.setStatus(PaymentStatus.SUCCESS);

        // WHY: Mock repository to return the payment.
        when(paymentRepository.findById(7L))
                .thenReturn(Optional.of(payment));

        // WHY: Verify that only PENDING payment can become FAILED.
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> paymentService.markPaymentFailed(7L)
                );

        // WHY: Verify the actual business validation message.
        assertEquals(
                "Only PENDING payment can be marked as FAILED",
                exception.getMessage(),
                exception.getMessage()
        );

        // WHY: Verify that invalid payment is not saved.
        verify(paymentRepository, never())
                .save(any(Payment.class));

        // WHY: Verify that no FAILED audit log is created.
        verify(auditLogService, never())
                .log(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }
    @Test
    void createPayment_shouldReturnExistingPayment_whenIdempotencyKeyAlreadyExists() {

        // WHY: Create an existing payment for the same idempotency key.
        Payment existingPayment = new Payment();

        existingPayment.setId(10L);
        existingPayment.setOrderId(1L);
        existingPayment.setUserEmail("pulak@gmail.com");
        existingPayment.setStatus(PaymentStatus.PENDING);
        existingPayment.setIdempotencyKey("KEY-123");

        // WHY: Mock repository to find the existing payment.
        when(paymentRepository.findByIdempotencyKey("KEY-123"))
                .thenReturn(Optional.of(existingPayment));

        // WHY: Create the payment request.
        PaymentRequestDto request = new PaymentRequestDto();
        request.setOrderId(1L);
        request.setIdempotencyKey("KEY-123");

// WHY: Call createPayment with the request.
        PaymentResponseDto response =
                paymentService.createPayment(
                        request,
                        "pulak@gmail.com"
                );

        // WHY: Verify that the existing payment is returned.
        assertEquals(
                existingPayment.getId(),
                response.getId()
        );

        // WHY: Verify that no new payment is created.
        verify(paymentRepository, never())
                .save(any(Payment.class));

        // WHY: Verify that no new audit log is created.
        verify(auditLogService, never())
                .log(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }
}