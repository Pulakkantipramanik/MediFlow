package com.mediflow.medicine.exception;
import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice //global exception handling from controller.one centrlize class
public class GlobalExceptionHandler {

    @ExceptionHandler(MedicineNotFoundException.class)// particular exception regarding which method will execute that define by this .
    public ResponseEntity<String> handleMedicineNotFound(MedicineNotFoundException ex  ) // handleMedicineNotFound- you can give any name
    {
        return  ResponseEntity // HTTP status + response body control
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }


    @ExceptionHandler(MethodArgumentNotValidException.class) // this class build in spring
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        // validation error রাখার জন্য Map তৈরি
        Map<String, String> errors = new HashMap<>();

        //  সব validation field error বের করে
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        // field name + error message Map-এ রাখে
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        // 400 Bad Request + validation errors client-কে return
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
        // Exception → Handle validation exception
// Map → Store errors
// BindingResult → Get validation result
// FieldErrors → Get field errors
// getField() → Field name
// getDefaultMessage() → Error message
// put() → Store error
// 400 → Bad Request
// body() → Return errors
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        // PURPOSE:
        // Return HTTP 400 because the request contains
        // an invalid value or business condition.
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }
    @ExceptionHandler(OptimisticLockException.class)
// PURPOSE:
// Handle concurrent database update conflicts.
//
// WHY:
// When two users try to update the same medicine at the
// same time, @Version detects the conflict.
// We return 409 instead of exposing a server error.
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(
            OptimisticLockException ex) {

        // PURPOSE:
        // Create a standard conflict response.
        //
        // WHY:
        // The request conflicts with a newer database state.
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Medicine stock was updated by another request. Please try again.",
                LocalDateTime.now(),
                null
        );

        // PURPOSE:
        // Return HTTP 409 CONFLICT.
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }
}
