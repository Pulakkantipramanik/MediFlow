package com.mediflow.medicine.exception;

// PURPOSE:
// Represents the situation where a requested payment
// does not exist in the database.
//
// WHY:
// Payment-related "not found" errors should return
// HTTP 404 instead of being treated as a generic error.
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}