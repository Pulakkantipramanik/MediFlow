package com.mediflow.prescription.dto;

import com.mediflow.prescription.entity.PrescriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponseDto {

    private Long id;

    private String userEmail;

    private Long medicineId;

    private String prescriptionFileName;

    private PrescriptionStatus status;

    private String rejectionReason;

    private LocalDateTime uploadedAt;
}