package com.mediflow.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@AllArgsConstructor
public class OrderResponseDto {

    private Long id;

    private String userEmail;

    private Long medicineId;

    private Integer quantity;

    private BigDecimal totalPrice;

    private String status;

    private LocalDateTime orderDate;

    private String rejectionReason;
}