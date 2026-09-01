package com.mediflow.payment.repository;

import com.mediflow.payment.entity.Payment;
import com.mediflow.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    List<Payment> findByUserEmail(String userEmail);

    List<Payment> findByStatus(PaymentStatus status);

    // PURPOSE:
    // Fetch payments page-by-page instead of loading all records into memory.
    // WHY:
    // This is important when the payment table contains thousands or millions
    // of records.
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    // PURPOSE:
    // Find an existing payment using the idempotency key.
    //
    // WHY:
    // This allows us to detect repeated requests before
    // creating a new payment.
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

}