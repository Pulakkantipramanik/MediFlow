package com.mediflow.prescription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PrescriptionRejectRequestDto {

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}