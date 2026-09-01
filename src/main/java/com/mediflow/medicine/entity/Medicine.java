package com.mediflow.medicine.entity;


import com.mediflow.medicine.enums.MedicineStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Medicine name is required")
    private String name ;
    private String description;
    private String category;
    private String manufacturer;

    @NotNull(message = "Price is Required")
    @Positive(message = "Price must be greater than zero")
    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0 , message =" stock can not be negative")
    @Column(nullable = false)
    private Integer  stockQuantity;

    private LocalDate expiryDate;
    private Boolean prescriptionRequired;

    @Enumerated(EnumType.STRING)
    private MedicineStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime    updatedAt;

}
