package com.mediflow.payment.dto;

import com.mediflow.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private Long id;

    private Long orderId;

    private String userEmail;

    private BigDecimal amount;

    private PaymentStatus status;

    private String transactionId;

    private LocalDateTime paymentDate;

    // PURPOSE:
    // Stores the unique key used for the payment request.
    //
    // WHY:
    // This value is used for idempotency, so if the same payment
    // request is sent again, we can identify the existing payment.
    private String idempotencyKey;
}