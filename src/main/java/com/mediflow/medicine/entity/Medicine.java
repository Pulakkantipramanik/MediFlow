package com.mediflow.medicine.entity;

import jakarta.persistence.Version;
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

    // PURPOSE:
// Used by Hibernate for optimistic locking.
//
// WHY:
// Multiple users may try to update the same medicine stock
// at the same time.
//
// Hibernate checks the version before updating the row.
// If another transaction already changed the row,
// Hibernate detects the conflict instead of silently
// overwriting the latest stock value.
    @Version
    private Long version;

}
