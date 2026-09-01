package com.mediflow.prescription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PrescriptionRequestDto {

    @NotNull(message = "Medicine ID is required")
    private Long medicineId;
    @NotNull(message = "Prescription file is required")
    private MultipartFile file;
}