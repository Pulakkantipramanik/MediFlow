package com.mediflow.prescription.repository;

import com.mediflow.prescription.entity.Prescription;
import com.mediflow.prescription.entity.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository
        extends JpaRepository<Prescription, Long> {

    List<Prescription> findByUserEmail(String userEmail);

    List<Prescription> findByStatus(PrescriptionStatus status);

    Optional<Prescription> findByUserEmailAndMedicineIdAndStatus(
            String userEmail,
            Long medicineId,
            PrescriptionStatus status
    );
}