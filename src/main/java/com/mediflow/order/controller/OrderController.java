package com.mediflow.order.controller;

import com.mediflow.order.dto.OrderRejectRequestDto;
import com.mediflow.order.dto.OrderRequestDto;
import com.mediflow.order.dto.OrderResponseDto;
import com.mediflow.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;


    // PURPOSE:
    // Creates a new order for the currently authenticated user.
    //
    // WHY:
    // The logged-in user's email is taken from the JWT Authentication
    // instead of accepting userEmail from the request body.
    //
    // ACCESS:
    // Both USER and ADMIN can create an order.
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @Valid @RequestBody OrderRequestDto request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        OrderResponseDto response =
                orderService.createOrder(
                        request,
                        userEmail
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // PURPOSE:
    // Returns all orders belonging to the currently authenticated user.
    //
    // WHY:
    // A user should only be able to see their own orders.
    //
    // ACCESS:
    // Both USER and ADMIN can use this endpoint to view their own orders.
    // PURPOSE:
// Returns all orders belonging to the currently authenticated user.
//
// WHY:
// A user should only be able to view their own orders.
// The email is taken from JWT Authentication instead of
// accepting it from the client.
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(
            Authentication authentication) {

        String userEmail = authentication.getName();

        // PURPOSE:
        // Fetch the authenticated user's orders from the service.
        //
        // WHY:
        // The service uses the email to filter orders
        // belonging to this logged-in user.
        List<OrderResponseDto> orders =
                orderService.getMyOrders(userEmail);

        return ResponseEntity.ok(orders);

    }


    // PURPOSE:
    // Allows a user to cancel their own pending order.
    //
    // WHY:
    // The user's email comes from Authentication so that the service
    // can verify that the order belongs to the logged-in user.
    //
    // ACCESS:
    // Authenticated USER or ADMIN.
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();

        OrderResponseDto response =
                orderService.cancelOrder(
                        id,
                        userEmail
                );

        return ResponseEntity.ok(response);
    }


    // PURPOSE:
    // Returns all orders in the system.
    //
    // WHY:
    // ADMIN needs to manage and review orders from all users.
    //
    // ACCESS:
    // ADMIN only.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderResponseDto>> getAllOrders(
            Pageable pageable) {

        Page<OrderResponseDto> orders =
                orderService.getAllOrders(pageable);

        return ResponseEntity.ok(orders);
    }


    // PURPOSE:
    // Approves a pending order.
    //
    // WHY:
    // The ADMIN email is extracted from Authentication
    // so that the approval action can be recorded in audit logs.
    //
    // ACCESS:
    // ADMIN only.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<OrderResponseDto> approveOrder(
            @PathVariable Long id,
            Authentication authentication) {

        String adminEmail = authentication.getName();

        OrderResponseDto response =
                orderService.approveOrder(
                        id,
                        adminEmail
                );

        return ResponseEntity.ok(response);
    }


    // PURPOSE:
    // Moves a PROCESSING order to SHIPPED.
    //
    // WHY:
    // Only ADMIN should control the shipment stage.
    // ADMIN email is also required for audit logging.
    //
    // ACCESS:
    // ADMIN only.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/ship")
    public ResponseEntity<OrderResponseDto> shipOrder(
            @PathVariable Long id,
            Authentication authentication) {

        String adminEmail = authentication.getName();

        OrderResponseDto response =
                orderService.shipOrder(
                        id,
                        adminEmail
                );

        return ResponseEntity.ok(response);
    }


    // PURPOSE:
    // Moves a SHIPPED order to DELIVERED.
    //
    // WHY:
    // Delivery is an administrative fulfillment action
    // and must be tracked in the audit history.
    //
    // ACCESS:
    // ADMIN only.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/deliver")
    public ResponseEntity<OrderResponseDto> deliverOrder(
            @PathVariable Long id,
            Authentication authentication) {

        String adminEmail = authentication.getName();

        OrderResponseDto response =
                orderService.deliverOrder(
                        id,
                        adminEmail
                );

        return ResponseEntity.ok(response);
    }


    // PURPOSE:
    // Rejects a pending order and stores the rejection reason.
    //
    // WHY:
    // The rejection reason comes from the request body,
    // while the ADMIN email comes from Authentication.
    // Both are required by the service for rejection and audit logging.
    //
    // ACCESS:
    // ADMIN only.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<OrderResponseDto> rejectOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderRejectRequestDto request,
            Authentication authentication) {

        String adminEmail = authentication.getName();

        OrderResponseDto response =
                orderService.rejectOrder(
                        id,
                        request,
                        adminEmail
                );

        return ResponseEntity.ok(response);
    }

}