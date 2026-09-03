package com.mediflow.audit.service;

import com.mediflow.audit.entity.AuditLog;
import com.mediflow.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // PURPOSE:
    // Creates and stores an audit log for a business action.
    //
    // WHY:
    // Keeping audit creation in a separate service allows
    // OrderService, PaymentService and PrescriptionService
    // to reuse the same audit logging logic.
    public void log(
            String userEmail,
            String action,
            String entityType,
            Long entityId,
            String description) {

        AuditLog auditLog = new AuditLog();

        auditLog.setUserEmail(userEmail);
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDescription(description);

        // PURPOSE:
        // Record the exact time when the business action occurred.
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
    }
    // PURPOSE:
// Returns the audit history of a specific entity.
//
// WHY:
// ADMIN should be able to see all actions performed
// on a particular order for traceability.
    public List<AuditLog> getEntityAuditLogs(
            String entityType,
            Long entityId) {

        return auditLogRepository.findByEntityTypeAndEntityId(
                entityType,
                entityId
        );
    }
}