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
                        request.getRejectionReason()
                );

        return ResponseEntity.ok(response);
    }
}