package com.mediflow.medicine.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    // PURPOSE:
    // HTTP status code returned to the client.
    private int status;

    // PURPOSE:
    // Human-readable error message.
    private String message;

    // PURPOSE:
    // Helps identify when the error occurred.
    private LocalDateTime timestamp;

    // PURPOSE:
    // Store field-level validation errors.
    //
    // EXAMPLE:
    // email -> "Email is required"
    // password -> "Password is required"
    //
    // WHY:
    // Validation can produce multiple errors at the same time.
    private Map<String, String> errors;
}