package com.mediflow.medicine.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }
}
