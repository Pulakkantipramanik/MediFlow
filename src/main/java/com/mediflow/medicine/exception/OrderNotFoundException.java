package com.mediflow.medicine.exception;

// PURPOSE:
// Represents the situation where the requested order
// does not exist in the database.
//
// WHY:
// A separate exception makes the error more meaningful
// and allows GlobalExceptionHandler to return HTTP 404.
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }
}