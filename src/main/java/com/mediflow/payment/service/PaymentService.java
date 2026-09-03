package com.mediflow.payment.service;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;


    // PURPOSE:
    // Constructor injection is used to inject the required repositories.
    //
    // WHY:
    // Constructor injection makes dependencies explicit and makes the
    // service easier to test.
    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository) {

        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
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
                        new IllegalArgumentException(
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


        // BUSINESS RULE:
        // Only a PENDING payment can become SUCCESS.
        //
        // WHY:
        // Prevents an already SUCCESS or FAILED payment from
        // being changed incorrectly.
        if (payment.getStatus() != PaymentStatus.PENDING) {

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


        // BUSINESS RULE:
        // Only a PENDING payment can become FAILED.
        //
        // WHY:
        // Prevents an already completed payment from being
        // changed to FAILED.
        if (payment.getStatus() != PaymentStatus.PENDING) {

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


        // PURPOSE:
        // Return the updated payment as a DTO.
        return mapToResponseDto(updatedPayment);
    }

    // ============================================================
// PAYMENT WEBHOOK
// ============================================================

    // PURPOSE:
// Processes the payment result received from the payment gateway.
//
// BUSINESS FLOW:
// Find Payment
// → Validate PENDING Status
// → Validate Gateway Status
// → Set Transaction ID
// → Update Payment Status
// → Save Payment.
//
// WHY:
// In a real payment application, the payment gateway informs our
// application about the final payment result through a webhook.
    @Transactional
    public PaymentResponseDto processWebhook(
            PaymentWebhookRequestDto request) {

        // PURPOSE:
        // Find the payment that the gateway is sending an update for.
        //
        // WHY:
        // We cannot update the payment status if the payment does not exist.
        Payment payment = paymentRepository
                .findById(request.getPaymentId())
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found with id: "
                                        + request.getPaymentId()));


        // BUSINESS RULE:
        // Only a PENDING payment can be updated by the webhook.
        //
        // WHY:
        // Once the payment is SUCCESS or FAILED, it should not be
        // changed again by another webhook request.
        if (payment.getStatus() != PaymentStatus.PENDING) {

            throw new IllegalArgumentException(
                    "Only PENDING payment can be updated by webhook");
        }


        // BUSINESS RULE:
        // The webhook should only accept SUCCESS or FAILED status.
        //
        // WHY:
        // The webhook represents the final result of a payment attempt.
        payment.setStatus(request.getStatus());
        if (request.getStatus() != PaymentStatus.SUCCESS
                && request.getStatus() != PaymentStatus.FAILED) {

            throw new IllegalArgumentException(
                    "Webhook status must be SUCCESS or FAILED");
        }


        // PURPOSE:
        // Store the transaction ID received from the payment gateway.
        //
        // WHY:
        // This transaction ID is the reference provided by the external
        // payment provider for tracking the payment.
        payment.setTransactionId(
                request.getTransactionId());


        // PURPOSE:
        // Update the payment status with the result received
        // from the payment gateway.
        payment.setStatus(
                request.getStatus());


        // PURPOSE:
        // Save the updated payment record in the database.
        Payment updatedPayment =
                paymentRepository.save(payment);


        // PURPOSE:
        // Return the updated payment as a DTO.
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


    // ============================================================
    // GET PAYMENT BY ID
    // ============================================================

    // PURPOSE:
    // Returns one payment by ID for the authenticated user.
    //
    // SECURITY:
    // The payment must belong to the authenticated user.
    //
    // BUSINESS RULE:
    // A user cannot access another user's payment.
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
                                        + paymentId));

        // BUSINESS RULE:
// A payment can be processed only once.
//
// WHY:
// Payment gateways may send the same webhook multiple times.
// If the payment already has a transaction ID, it means
// the webhook has already been processed.
        if (payment.getTransactionId() != null
                && !payment.getTransactionId().isBlank()) {

            throw new IllegalArgumentException(
                    "Webhook already processed for payment id: "
                            + payment.getId());
        }


        // SECURITY:
        // Verify that the payment belongs to the authenticated user.
        //
        // WHY:
        // Prevents a user from accessing another user's payment
        // simply by changing the ID in the URL.
        if (!payment.getUserEmail().equals(userEmail)) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this payment");
        }


        // PURPOSE:
        // Convert the payment entity into a response DTO.
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