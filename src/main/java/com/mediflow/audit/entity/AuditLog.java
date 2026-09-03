package com.mediflow.audit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PURPOSE:
    // Stores the email of the user/admin who performed the action.
    //
    // WHY:
    // Audit logs must tell us WHO performed a business operation.
    private String userEmail;

    // PURPOSE:
    // Stores the type of business operation performed.
    //
    // Example:
    // ORDER_APPROVED, ORDER_REJECTED, ORDER_CANCELLED
    private String action;

    // PURPOSE:
    // Stores the type of entity on which the action was performed.
    //
    // Example:
    // ORDER, PAYMENT, PRESCRIPTION
    private String entityType;

    // PURPOSE:
    // Stores the ID of the affected entity.
    //
    // WHY:
    // This allows us to find exactly which order/payment/prescription
    // was affected by the action.
    private Long entityId;

    // PURPOSE:
    // Stores additional information about the action.
    //
    // Example:
    // "Order approved by admin"
    // "Order rejected because prescription was invalid"
    @Column(length = 1000)
    private String description;

    // PURPOSE:
    // Stores the exact time when the action happened.
    private LocalDateTime createdAt;
}