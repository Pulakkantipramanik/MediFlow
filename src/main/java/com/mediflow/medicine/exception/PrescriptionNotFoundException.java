package com.mediflow.medicine.exception;

// PURPOSE:
// Represents the situation where the requested prescription
// does not exist in the database.
//
// WHY:
// A separate exception makes prescription-related errors
// easier to identify and allows us to return HTTP 404.
public class PrescriptionNotFoundException extends RuntimeException {

    public PrescriptionNotFoundException(String message) {
        super(message);
    }
}