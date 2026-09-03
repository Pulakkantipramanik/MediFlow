package com.mediflow.securityconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediflow.medicine.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
// PURPOSE:
// Handles authentication failures globally.
//
// WHY:
// When a user does not provide a valid JWT,
// Spring Security should return a proper 401 response.
public class CustomAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {

        // PURPOSE:
        // Create a standard error response.
        //
        // WHY:
        // Security errors should use the same response
        // structure as our application exceptions.
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized: Please provide a valid JWT token",
                LocalDateTime.now(),
                null
        );

        // PURPOSE:
        // Set HTTP status to 401.
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        // PURPOSE:
        // Tell client that response is JSON.
        response.setContentType("application/json");

        // PURPOSE:
        // Convert ErrorResponse Java object into JSON
        // and write it to the HTTP response.
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.writeValue(
                response.getWriter(),
                errorResponse
        );
    }
}