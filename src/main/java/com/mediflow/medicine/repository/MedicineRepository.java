package com.mediflow.medicine.repository;

import com.mediflow.medicine.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine,Long>, JpaSpecificationExecutor<Medicine> {

    // JDBC code
    // Connection
    // PreparedStatement
    // ResultSet

    // api search
/*
    List<Medicine> findByNameContainingIgnoreCase(String name);
*/
   /* findBy        → Search
    Name          → Which field?
    Containing    → Partial match
    IgnoreCase    → Upper/lower case ignore
    String name   → Search value
    List<Medicine>→ Matching results*/
   Page<Medicine> findByNameContainingIgnoreCase(
           String name,
           Pageable pageable);

    Page<Medicine> findByCategoryIgnoreCase(String category, Pageable pageable);

    Page<Medicine> findByCategoryIgnoreCaseAndManufacturerIgnoreCaseAndPriceBetween(
            String category,
            String manufacturer,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable);


}
