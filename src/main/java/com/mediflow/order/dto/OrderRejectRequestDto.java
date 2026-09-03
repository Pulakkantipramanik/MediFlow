package com.mediflow.order.dto;

import jakarta.validation.constraints.NotBlank;

// PURPOSE:
// DTO used to receive the rejection reason from ADMIN.
//
// WHY:
// We don't send the complete Order entity from the request.
// We only need the rejection reason when rejecting an order.
public class OrderRejectRequestDto {

    // PURPOSE:
    // Stores the reason why the ADMIN rejected the order.
    //
    // WHY:
    // A rejection reason is mandatory, so we validate that
    // the value is not null, empty or only whitespace.
    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;

    // PURPOSE:
    // Returns the rejection reason as String.
    //
    // WHY:
    // Order.setRejectionReason() expects a String value.
    public String getRejectionReason() {
        return rejectionReason;
    }

    // PURPOSE:
    // Sets the rejection reason received from the API request.
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}