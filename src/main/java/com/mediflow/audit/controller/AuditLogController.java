package com.mediflow.audit.controller;

import com.mediflow.audit.entity.AuditLog;
import com.mediflow.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    // PURPOSE:
    // Allows ADMIN to view the complete audit history of an order.
    //
    // WHY:
    // Audit information may contain operational details,
    // so normal users should not have access to it.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<AuditLog>> getOrderAuditLogs(
            @PathVariable Long orderId) {

        List<AuditLog> auditLogs =
                auditLogService.getEntityAuditLogs(
                        "ORDER",
                        orderId
                );

        return ResponseEntity.ok(auditLogs);
    }

    // PURPOSE:
    // Allows ADMIN to view the complete audit history
    // of a prescription.
    //
    // WHY:
    // Prescription approval/rejection decisions should be
    // traceable for administrative review.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/prescriptions/{prescriptionId}")
    public ResponseEntity<List<AuditLog>> getPrescriptionAuditLogs(
            @PathVariable Long prescriptionId) {

        List<AuditLog> auditLogs =
                auditLogService.getEntityAuditLogs(
                        "PRESCRIPTION",
                        prescriptionId
                );

        return ResponseEntity.ok(auditLogs);
    }

    // PURPOSE:
    // Allows ADMIN to view the complete audit history
    // of a payment.
    //
    // WHY:
    // Payment activities such as creation, success,
    // failure, and webhook processing should be traceable
    // for administrative and troubleshooting purposes.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<List<AuditLog>> getPaymentAuditLogs(
            @PathVariable Long paymentId) {

        List<AuditLog> auditLogs =
                auditLogService.getEntityAuditLogs(
                        "PAYMENT",
                        paymentId
                );

        return ResponseEntity.ok(auditLogs);
    }
}