package com.mediflow.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    // PURPOSE:
    // User কোন order-এর জন্য payment initiate করতে চায় সেটা identify করার জন্য orderId নেওয়া হচ্ছে.
    @NotNull(message = "Order ID is required")
    private Long orderId;

    // PURPOSE:
    // Identifies one unique payment attempt.
    //
    // WHY:
    // The same key can be safely retried without creating
    // another payment record.
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;


}