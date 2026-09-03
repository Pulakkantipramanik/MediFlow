package com.mediflow.securityconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediflow.medicine.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
// PURPOSE:
// Handles authorization failures globally.
//
// WHY:
// A logged-in user may still not have permission
// to access a particular resource.
public class CustomAccessDeniedHandler
        implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {

        // PURPOSE:
        // Create a standard error response.
        //
        // WHY:
        // All API errors should follow a consistent format.
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden: You do not have permission to access this resource",
                LocalDateTime.now(),
                null
        );

        // PURPOSE:
        // Set HTTP status to 403.
        response.setStatus(HttpStatus.FORBIDDEN.value());

        // PURPOSE:
        // Tell client that response is JSON.
        response.setContentType("application/json");

        // PURPOSE:
        // Convert ErrorResponse object to JSON.
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.writeValue(
                response.getWriter(),
                errorResponse
        );
    }
}