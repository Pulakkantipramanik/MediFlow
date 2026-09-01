package com.mediflow.medicine.dto;

import com.mediflow.medicine.enums.MedicineStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
/*REQUEST:

UI → RequestDTO → Entity → DB


RESPONSE:

DB → Entity → ResponseDTO → UI*/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicineResponseDto {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String manufacturer;
    private BigDecimal price;
    private Integer stockQuantity;
    private LocalDate expiryDate;
    private Boolean prescriptionRequired;
    private MedicineStatus status;
}

