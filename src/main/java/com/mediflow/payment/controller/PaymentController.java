package com.mediflow.payment.controller;

import com.mediflow.payment.dto.PaymentRequestDto;
import com.mediflow.payment.dto.PaymentResponseDto;
import com.mediflow.payment.dto.PaymentWebhookRequestDto;
import com.mediflow.payment.entity.PaymentStatus;
import com.mediflow.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    // PURPOSE:
    // Reads the shared webhook secret from application.properties.
    //
    // WHY:
    // The secret is kept outside the Java code so it can be changed
    // through configuration.
    @Value("${payment.webhook.secret}")
    private String webhookSecret;


    // PURPOSE:
    // Creates a payment for the currently authenticated user.
    //
    // SECURITY:
    // User email comes from JWT/SecurityContext instead of request body.
    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody PaymentRequestDto request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        PaymentResponseDto response =
                paymentService.createPayment(request, userEmail);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // PURPOSE:
    // Allows a user to view their own payment history.
    //
    // SECURITY:
    // Email comes from the authenticated JWT.
    @GetMapping("/my-payments")
    public ResponseEntity<List<PaymentResponseDto>> getMyPayments(
            Authentication authentication) {

        String userEmail = authentication.getName();

        List<PaymentResponseDto> payments =
                paymentService.getMyPayments(userEmail);

        return ResponseEntity.ok(payments);
    }


    // PURPOSE:
    // Allows a user to view one of their own payments.
    //
    // SECURITY:
    // Service verifies payment ownership.
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> getPaymentById(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();

        PaymentResponseDto response =
                paymentService.getPaymentById(id, userEmail);

        return ResponseEntity.ok(response);
    }


    // PURPOSE:
    // Simulates successful payment processing.
    //
    // NOTE:
    // In a real payment system, this normally comes from
    // a payment gateway webhook/callback.
    @PatchMapping("/{id}/success")
    public ResponseEntity<PaymentResponseDto> markPaymentSuccess(
            @PathVariable Long id) {

        PaymentResponseDto response =
                paymentService.markPaymentSuccess(id);

        return ResponseEntity.ok(response);
    }


    // PURPOSE:
    // Simulates failed payment processing.
    //
    // NOTE:
    // In a real payment system, this normally comes from
    // a payment gateway response/webhook.
    @PatchMapping("/{id}/failed")
    public ResponseEntity<PaymentResponseDto> markPaymentFailed(
            @PathVariable Long id) {

        PaymentResponseDto response =
                paymentService.markPaymentFailed(id);

        return ResponseEntity.ok(response);
    }
    // ============================================================
// PAYMENT WEBHOOK
// ============================================================

    // PURPOSE:
// Receives the payment result from the payment gateway.
//
// BUSINESS FLOW:
// Payment Gateway
//      ↓
// POST /api/payments/webhook
//      ↓
// PaymentController
//      ↓
// PaymentService.processWebhook()
//      ↓
// Update Payment Status
//
// WHY:
// In a real payment system, the payment gateway sends the
// final payment result to our application through a webhook.
    // PURPOSE:
// Receives the payment result from the payment gateway.
//
// SECURITY:
// Requires a valid X-Webhook-Secret header.
//
// WHY:
// Without webhook authentication, anyone could call this endpoint
// and change a payment from PENDING to SUCCESS.
    @PostMapping("/webhook")
    public ResponseEntity<PaymentResponseDto> processWebhook(
            @RequestHeader("X-Webhook-Secret") String receivedSecret,
            @Valid @RequestBody PaymentWebhookRequestDto request) {

        // SECURITY:
        // Compare the secret received from the webhook request with
        // the trusted secret configured in the application.
        //
        // WHY:
        // Only requests containing the correct secret should be allowed
        // to update payment status.
        if (!webhookSecret.equals(receivedSecret)) {

            throw new IllegalArgumentException(
                    "Invalid webhook secret");
        }


        // PURPOSE:
        // Pass the validated webhook request to the service layer.
        //
        // WHY:
        // Payment business logic should remain inside the service layer.
        PaymentResponseDto response =
                paymentService.processWebhook(request);


        return ResponseEntity.ok(response);
    }


    // SECURITY:
    // Only ADMIN can view all payments.
    //
    // PAGINATION:
    // Example: ?page=0&size=5
    //
    // SORTING:
    // Example: ?sort=paymentDate,desc
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<PaymentResponseDto>> getAllPayments(
            Pageable pageable) {

        Page<PaymentResponseDto> response =
                paymentService.getAllPayments(pageable);

        return ResponseEntity.ok(response);
    }


    // SECURITY:
    // Only ADMIN can filter payments by status.
    //
    // PAGINATION + SORTING:
    // Pageable reads page, size and sort from the URL.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<PaymentResponseDto>> getPaymentsByStatus(
            @PathVariable PaymentStatus status,
            Pageable pageable) {

        Page<PaymentResponseDto> response =
                paymentService.getPaymentsByStatus(status, pageable);

        return ResponseEntity.ok(response);
    }
}