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

    // USER + ADMIN → Create Order
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

    // USER + ADMIN → View their own orders
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(
            Authentication authentication) {

        String userEmail = authentication.getName();

        List<OrderResponseDto> orders =
                orderService.getMyOrders(userEmail);

        return ResponseEntity.ok(orders);
    }
    // PURPOSE:
// Allows a logged-in user to cancel their own pending order.
//
// WHY:
// Users should be able to cancel an order before admin approval.
// Authentication is used to identify the logged-in user.
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();

        OrderResponseDto response =
                orderService.cancelOrder(id, userEmail);

        return ResponseEntity.ok(response);
    }

    // ADMIN only → View all orders
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderResponseDto>> getAllOrders(
            Pageable pageable) {

        Page<OrderResponseDto> orders =
                orderService.getAllOrders(pageable);

        return ResponseEntity.ok(orders);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<OrderResponseDto> approveOrder(
            @PathVariable Long id) {

        OrderResponseDto response =
                orderService.approveOrder(id);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<OrderResponseDto> rejectOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderRejectRequestDto request) {

        OrderResponseDto response =
                orderService.rejectOrder(
                        id,
                        request
                );

        return ResponseEntity.ok(response);
    }
    // PURPOSE:
// Allows only ADMIN to mark a processing order as shipped.
//
// WHY:
// Shipping is an operational action and should not be
// controlled by a normal customer.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/ship")
    public ResponseEntity<OrderResponseDto> shipOrder(
            @PathVariable Long id) {

        OrderResponseDto response =
                orderService.shipOrder(id);

        return ResponseEntity.ok(response);
    }
    // PURPOSE:
// Allows only ADMIN to mark a shipped order as delivered.
//
// WHY:
// Delivery is the final fulfillment step of the order.
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/deliver")
    public ResponseEntity<OrderResponseDto> deliverOrder(
            @PathVariable Long id) {

        OrderResponseDto response =
                orderService.deliverOrder(id);

        return ResponseEntity.ok(response);
    }

}