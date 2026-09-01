package com.mediflow.prescription.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;

    private Long medicineId;

    private String prescriptionFileName;

    @Enumerated(EnumType.STRING)
    private PrescriptionStatus status;

    @Column(length = 500)
    private String rejectionReason;

    private LocalDateTime uploadedAt;
}