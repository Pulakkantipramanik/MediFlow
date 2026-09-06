package com.mediflow.payment.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mediflow.medicine.exception.OrderNotFoundException;
import com.mediflow.medicine.exception.PaymentNotFoundException;
import com.mediflow.order.entity.Order;
import com.mediflow.order.entity.OrderStatus;
import com.mediflow.order.repository.OrderRepository;
import com.mediflow.payment.dto.PaymentRequestDto;
import com.mediflow.payment.dto.PaymentResponseDto;
import com.mediflow.payment.entity.Payment;
import com.mediflow.payment.entity.PaymentStatus;
import com.mediflow.payment.repository.PaymentRepository;
import com.mediflow.payment.dto.PaymentWebhookRequestDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mediflow.audit.service.AuditLogService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuditLogService auditLogService;

    private static final Logger log =
            LoggerFactory.getLogger(PaymentService.class);


    // PURPOSE:
    // Constructor injection is used to inject the required repositories.
    //
    // WHY:
    // Constructor injection makes dependencies explicit and makes the
    // service easier to test.
    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            AuditLogService auditLogService) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.auditLogService = auditLogService;
    }

    // ============================================================
    // CREATE PAYMENT
    // ============================================================

    // PURPOSE:
    // Creates a PENDING payment for an approved order.
    //
    // BUSINESS FLOW:
    // Idempotency Check
    // → Find Order
    // → Validate Ownership
    // → Validate Order Status
    // → Check Existing Payment
    // → Create Payment
    // → Save Payment.
    //
    // WHY:
    // Payment should only be initiated after ADMIN approves the order.
    @Transactional
    public PaymentResponseDto createPayment(
            PaymentRequestDto request,
            String userEmail) {

        // PURPOSE:
        // Check whether this exact payment request was already processed.
        //
        // WHY:
        // A client may retry the same request because of network problems
        // or because the user clicked the payment button multiple times.
        //
        // BUSINESS RULE:
        // The same idempotency key must not create another payment.
        // PURPOSE:
// Check whether this idempotency key was already used.
//
// WHY:
// A client may retry the same payment request.
// We should return the original payment instead of creating
// a duplicate payment.
        Optional<Payment> existingPayment =
                paymentRepository.findByIdempotencyKey(
                        request.getIdempotencyKey());


// BUSINESS RULE:
// If the idempotency key already exists, it must belong
// to the same user and same order.
//
// WHY:
// This prevents another user or another order from
// reusing an existing idempotency key.
        if (existingPayment.isPresent()) {

            Payment existingPaymentRecord = existingPayment.get();


            // SECURITY CHECK:
            // Verify that the existing payment belongs to
            // the currently authenticated user.
            if (!existingPaymentRecord.getUserEmail().equals(userEmail)) {

                throw new IllegalArgumentException(
                        "Idempotency key already belongs to another user");
            }


            // BUSINESS RULE:
            // The same idempotency key cannot be used
            // for a different order.
            if (!existingPaymentRecord.getOrderId()
                    .equals(request.getOrderId())) {

                throw new IllegalArgumentException(
                        "Idempotency key already belongs to another order");
            }


            // PURPOSE:
            // Return the original payment for a legitimate retry.
            //
            // WHY:
            // This is the actual idempotent behavior.
            return mapToResponseDto(existingPaymentRecord);
        }

        // PURPOSE:
        // Find the order that the user wants to pay for.
        //
        // WHY:
        // We need the order details such as amount and status
        // before creating the payment.
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order not found with id: "
                                        + request.getOrderId()));


        // SECURITY:
        // Verify that the authenticated user owns this order.
        //
        // WHY:
        // A user must not be able to make a payment for another
        // user's order.
        if (!order.getUserEmail().equals(userEmail)) {

            throw new IllegalArgumentException(
                    "You are not authorized to pay for this order");
        }


        // BUSINESS RULE:
        // Payment is allowed only after ADMIN approves the order.
        //
        // WHY:
        // PENDING or REJECTED orders should not be paid for.
        if (order.getStatus() != OrderStatus.APPROVED) {

            throw new IllegalArgumentException(
                    "Payment can only be made for an APPROVED order");
        }


        // BUSINESS RULE:
        // Only one payment record is allowed for one order.
        //
        // WHY:
        // Prevents multiple payment records for the same order.
        if (paymentRepository.findByOrderId(order.getId()).isPresent()) {

            throw new IllegalArgumentException(
                    "Payment already exists for this order");
        }


        // PURPOSE:
        // Create a new Payment entity.
        Payment payment = new Payment();

        payment.setOrderId(order.getId());
        payment.setUserEmail(userEmail);


        // SECURITY / BUSINESS RULE:
        // Amount comes from the order stored in the database.
        //
        // WHY:
        // Never trust the payment amount from the client.
        // Otherwise, a user could try to modify the payment amount.
        payment.setAmount(order.getTotalPrice());


        // PURPOSE:
        // Every newly created payment starts in PENDING state.
        payment.setStatus(PaymentStatus.PENDING);


        // PURPOSE:
        // Store the date and time when the payment was created.
        payment.setPaymentDate(LocalDateTime.now());


        // PURPOSE:
        // Store the unique idempotency key.
        //
        // WHY:
        // If the same request comes again, we can identify the
        // existing payment using this key.
        payment.setIdempotencyKey(
                request.getIdempotencyKey());


        // PURPOSE:
        // Save the payment record in the database.
        Payment savedPayment =
                paymentRepository.save(payment);

        // PURPOSE:
// Log successful payment creation.
//
// WHY:
// Helps trace payment creation during development
// without logging sensitive payment information.
        log.info(
                "Payment created: paymentId={}, orderId={}, user={}",
                savedPayment.getId(),
                savedPayment.getOrderId(),
                userEmail
        );

        // PURPOSE:
// Record that the user created a payment.
//
// WHY:
// Payment creation is an important financial event
// and should be traceable in the audit history.
        auditLogService.log(
                userEmail,
                "PAYMENT_CREATED",
                "PAYMENT",
                savedPayment.getId(),
                "Payment created for order: "
                        + savedPayment.getOrderId()
        );


        // PURPOSE:
        // Return the saved payment as a DTO.
        return mapToResponseDto(savedPayment);
    }



    // ============================================================
    // PAYMENT ENTITY → RESPONSE DTO
    // ============================================================

    // PURPOSE:
    // Converts Payment entity into PaymentResponseDto.
    //
    // WHY:
    // We return a DTO instead of directly exposing the JPA entity.
    //
    // BENEFIT:
    // This keeps database entities separate from API response objects.
    private PaymentResponseDto mapToResponseDto(
            Payment payment) {

        return new PaymentResponseDto(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserEmail(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getPaymentDate(),
                payment.getIdempotencyKey()
        );
    }


    // ============================================================
    // PAYMENT SUCCESS
    // ============================================================

    // PURPOSE:
    // Marks a PENDING payment as SUCCESS.
    //
    // BUSINESS FLOW:
    // Find Payment
    // → Validate PENDING
    // → Generate Transaction ID
    // → Set SUCCESS
    // → Save Payment.
    //
    // BUSINESS RULE:
    // Only PENDING payments can become SUCCESS.
    //
    // WHY:
    // A SUCCESS or FAILED payment should not be processed again.
    @Transactional
    public PaymentResponseDto markPaymentSuccess(
            Long paymentId) {

        // PURPOSE:
        // Find the payment that needs to be marked as successful.
        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found with id: "
                                        + paymentId));


        if (payment.getStatus() != PaymentStatus.PENDING) {

            // PURPOSE:
            // Log an invalid payment state transition attempt.
            //
            // WHY:
            // Helps identify unexpected payment flows or
            // repeated status-change requests.
            log.warn(
                    "Invalid payment success attempt: paymentId={}, currentStatus={}",
                    paymentId,
                    payment.getStatus()
            );

            throw new IllegalArgumentException(
                    "Only PENDING payment can be marked as SUCCESS");
        }

        // PURPOSE:
        // Generate a transaction reference for the successful payment.
        //
        // WHY:
        // A transaction ID helps us track a particular payment.
        String transactionId =
                "TXN-" + System.currentTimeMillis();

        payment.setTransactionId(transactionId);


        // BUSINESS RULE:
        // Change payment state from PENDING to SUCCESS.
        payment.setStatus(PaymentStatus.SUCCESS);


        // PURPOSE:
        // Save the updated payment.
        Payment updatedPayment =
                paymentRepository.save(payment);


        // PURPOSE:
// Log successful payment status update.
//
// WHY:
// Helps trace when a payment changes from PENDING
// to SUCCESS.
        log.info(
                "Payment marked SUCCESS: paymentId={}, transactionId={}",
                updatedPayment.getId(),
                updatedPayment.getTransactionId()
        );
        // PURPOSE:
// Record successful payment processing.
//
// WHY:
// Payment success is a critical financial event
// and must be traceable.
        auditLogService.log(
                payment.getUserEmail(),
                "PAYMENT_SUCCESS",
                "PAYMENT",
                updatedPayment.getId(),
                "Payment marked as successful"
        );


        // PURPOSE:
        // Return the updated payment as a DTO.
        return mapToResponseDto(updatedPayment);
    }


    // ============================================================
    // PAYMENT FAILED
    // ============================================================

    // PURPOSE:
    // Marks a PENDING payment as FAILED.
    //
    // BUSINESS FLOW:
    // Find Payment
    // → Validate PENDING
    // → Set FAILED
    // → Save Payment.
    //
    // BUSINESS RULE:
    // Only PENDING payments can become FAILED.
    //
    // WHY:
    // A SUCCESS payment should not be changed to FAILED.
    @Transactional
    public PaymentResponseDto markPaymentFailed(
            Long paymentId) {

        // PURPOSE:
        // Find the payment that needs to be marked as failed.
        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found with id: "
                                        + paymentId));


        if (payment.getStatus() != PaymentStatus.PENDING) {

            // PURPOSE:
            // Log an invalid payment failure attempt.
            //
            // WHY:
            // Helps identify attempts to change an already
            // completed payment.
            log.warn(
                    "Invalid payment failure attempt: paymentId={}, currentStatus={}",
                    paymentId,
                    payment.getStatus()
            );

            throw new IllegalArgumentException(
                    "Only PENDING payment can be marked as FAILED");
        }


        // BUSINESS RULE:
        // Change payment state from PENDING to FAILED.
        payment.setStatus(PaymentStatus.FAILED);


        // PURPOSE:
        // Save the updated payment status.
        Payment updatedPayment =
                paymentRepository.save(payment);

        // Payment failures are important for troubleshooting
// and monitoring.
        log.info(
                "Payment marked FAILED: paymentId={}",
                updatedPayment.getId()
        );
        // PURPOSE:
// Record payment failure.
//
// WHY:
// Payment failures are important for debugging,
// monitoring and financial reconciliation.
        auditLogService.log(
                payment.getUserEmail(),
                "PAYMENT_FAILED",
                "PAYMENT",
                updatedPayment.getId(),
                "Payment marked as failed"
        );


        // PURPOSE:
        // Return the updated payment as a DTO.
        return mapToResponseDto(updatedPayment);
    }

    // PURPOSE:
// Processes the payment result received from the payment gateway.
//
// BUSINESS FLOW:
// Find Payment
// → Validate PENDING
// → Validate SUCCESS/FAILED
// → Set Transaction ID
// → Update Payment
// → If SUCCESS, move Order to PROCESSING
// → Save Payment
// → Create Audit Log.
//
// WHY:
// The payment gateway sends the final payment result
// asynchronously through a webhook.
    @Transactional
    public PaymentResponseDto processWebhook(
            PaymentWebhookRequestDto request) {

        // PURPOSE:
// Log that a payment webhook was received.
//
// WHY:
// Helps trace asynchronous payment gateway events.
        log.info(
                "Payment webhook received: paymentId={}, status={}",
                request.getPaymentId(),
                request.getStatus()
        );

        // PURPOSE:
        // Find the payment that the gateway is updating.
        Payment payment = paymentRepository
                .findById(request.getPaymentId())
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found with id: "
                                        + request.getPaymentId()
                        )
                );

        // BUSINESS RULE:
        // Only PENDING payments can be updated by webhook.
        //
        // WHY:
        // Once payment becomes SUCCESS or FAILED,
        // it should not be changed again.
        if (payment.getStatus() != PaymentStatus.PENDING) {

            throw new IllegalArgumentException(
                    "Only PENDING payment can be updated by webhook"
            );
        }

        // BUSINESS RULE:
        // Webhook can only send SUCCESS or FAILED.
        //
        // WHY:
        // These represent the final result of the payment attempt.
        if (request.getStatus() != PaymentStatus.SUCCESS
                && request.getStatus() != PaymentStatus.FAILED) {

            // PURPOSE:
            // Log an invalid webhook status.
            //
            // WHY:
            // Helps detect malformed or unexpected gateway events.
            log.warn(
                    "Invalid payment webhook status: paymentId={}, status={}",
                    request.getPaymentId(),
                    request.getStatus()
            );

            throw new IllegalArgumentException(
                    "Webhook status must be SUCCESS or FAILED"
            );
        }

        // PURPOSE:
        // Store the transaction ID received from the payment gateway.
        //
        // WHY:
        // This ID is the external payment provider's reference
        // for tracking the transaction.
        payment.setTransactionId(
                request.getTransactionId()
        );

        // PURPOSE:
        // Update payment status after validating the webhook status.
        payment.setStatus(request.getStatus());


        // BUSINESS RULE:
        // A successful payment moves the APPROVED order
        // into PROCESSING state.
        //
        // WHY:
        // The order should only start processing after
        // successful payment confirmation.
        if (request.getStatus() == PaymentStatus.SUCCESS) {

            Order order = orderRepository
                    .findById(payment.getOrderId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Order not found with id: "
                                            + payment.getOrderId()
                            )
                    );

            // BUSINESS RULE:
            // Only an APPROVED order can move to PROCESSING.
            //
            // WHY:
            // This prevents an invalid order state transition.
            if (order.getStatus() != OrderStatus.APPROVED) {

                throw new IllegalArgumentException(
                        "Only APPROVED order can move to PROCESSING"
                );
            }

            order.setStatus(OrderStatus.PROCESSING);

            orderRepository.save(order);
        }


        // PURPOSE:
        // Save the final payment status.
        Payment updatedPayment =
                paymentRepository.save(payment);

        // PURPOSE:
// Log successful webhook processing.
//
// WHY:
// Helps trace the final payment state received
// from the payment gateway.
        log.info(
                "Payment webhook processed: paymentId={}, status={}",
                updatedPayment.getId(),
                updatedPayment.getStatus()
        );

        // PURPOSE:
        // Record that the external payment webhook was processed.
        //
        // WHY:
        // Webhook events are important for payment reconciliation,
        // debugging and audit history.
        auditLogService.log(
                payment.getUserEmail(),
                "PAYMENT_WEBHOOK_PROCESSED",
                "PAYMENT",
                updatedPayment.getId(),
                "Webhook processed with status: "
                        + request.getStatus()
        );

        return mapToResponseDto(updatedPayment);

    }



    // ============================================================
    // MY PAYMENTS
    // ============================================================

    // PURPOSE:
    // Returns all payments belonging to the authenticated user.
    //
    // SECURITY:
    // userEmail comes from the authenticated JWT.
    //
    // WHY:
    // A user should only see their own payment history.
    public List<PaymentResponseDto> getMyPayments(
            String userEmail) {

        // PURPOSE:
        // Fetch only payments belonging to this user.
        List<Payment> payments =
                paymentRepository.findByUserEmail(userEmail);


        // PURPOSE:
        // Convert Payment entities into response DTOs.
        return payments.stream()
                .map(this::mapToResponseDto)
                .toList();
    }



    // PURPOSE:
// Returns one payment by ID for the authenticated user.
//
// SECURITY:
// The payment must belong to the authenticated user.
//
// WHY:
// A user cannot access another user's payment
// by changing the payment ID in the URL.
    public PaymentResponseDto getPaymentById(
            Long paymentId,
            String userEmail) {

        // PURPOSE:
        // Find the requested payment.
        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found with id: "
                                        + paymentId
                        )
                );

        // SECURITY:
        // Verify that the payment belongs to the authenticated user.
        //
        // WHY:
        // Prevents a user from accessing another user's payment.
        if (!payment.getUserEmail().equals(userEmail)) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this payment"
            );
        }

        return mapToResponseDto(payment);
    }

    // ============================================================
    // ADMIN - ALL PAYMENTS
    // ============================================================

    // PURPOSE:
    // Returns all payments for ADMIN with pagination and sorting.
    //
    // SECURITY:
    // The controller uses @PreAuthorize("hasRole('ADMIN')").
    //
    // WHY:
    // ADMIN may need to monitor payments from all users.
    //
    // PERFORMANCE:
    // Pageable prevents loading every payment record into memory.
    public Page<PaymentResponseDto> getAllPayments(
            Pageable pageable) {

        // PURPOSE:
        // Fetch only the requested page from the database.
        Page<Payment> payments =
                paymentRepository.findAll(pageable);


        // PURPOSE:
        // Convert each Payment entity into PaymentResponseDto
        // while keeping pagination information.
        return payments.map(this::mapToResponseDto);
    }


    // ============================================================
    // ADMIN - PAYMENTS BY STATUS
    // ============================================================

    // PURPOSE:
    // Returns payments filtered by status with pagination and sorting.
    //
    // EXAMPLES:
    // PENDING → Pending payments
    // SUCCESS → Successful payments
    // FAILED  → Failed payments
    //
    // SECURITY:
    // Only ADMIN can access this method through the controller.
    public Page<PaymentResponseDto> getPaymentsByStatus(
            PaymentStatus status,
            Pageable pageable) {

        // PURPOSE:
        // Fetch only payments matching the requested status.
        Page<Payment> payments =
                paymentRepository.findByStatus(
                        status,
                        pageable);


        // PURPOSE:
        // Convert Payment entities into response DTOs
        // while preserving pagination information.
        return payments.map(this::mapToResponseDto);
    }

}