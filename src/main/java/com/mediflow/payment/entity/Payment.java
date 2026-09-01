package com.mediflow.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // PURPOSE:
    // কোন order-এর জন্য payment তৈরি হয়েছে সেটা identify করার জন্য orderId রাখা হচ্ছে.
    private Long orderId;

    // PURPOSE:
    // কোন authenticated user payment করছে সেটা track করার জন্য email রাখা হচ্ছে.
    private String userEmail;

    // PURPOSE:
    // Order-এর payable amount payment record-এর সাথে সংরক্ষণ করার জন্য.
    private BigDecimal amount;

    // PURPOSE:
    // Payment বর্তমানে PENDING, SUCCESS অথবা FAILED কিনা সেটা store করার জন্য.
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    // PURPOSE:
    // Payment transaction-এর unique reference future payment gateway integration-এর জন্য রাখা হচ্ছে.
    private String transactionId;

    // PURPOSE:
    // Payment কখন তৈরি/initiate হয়েছে সেটা track করার জন্য.
    private LocalDateTime paymentDate;
    // PURPOSE:
    // Stores a unique key generated/provided for one payment request.
    //
    // WHY:
    // If the same payment request is sent again, we can identify it
    // using this key and avoid creating a duplicate payment.
    @Column(unique = true, nullable = false)
    private String idempotencyKey;

}