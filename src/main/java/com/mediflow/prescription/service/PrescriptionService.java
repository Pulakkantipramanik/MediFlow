package com.mediflow.prescription.service;

import com.mediflow.medicine.exception.PrescriptionNotFoundException;
import com.mediflow.prescription.dto.PrescriptionRequestDto;
import com.mediflow.prescription.dto.PrescriptionResponseDto;
import com.mediflow.prescription.entity.Prescription;
import com.mediflow.prescription.entity.PrescriptionStatus;
import com.mediflow.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    @Value("${prescription.upload-dir}")
    private String uploadDir;


    // UPLOAD PRESCRIPTION
    public PrescriptionResponseDto uploadPrescription(
            PrescriptionRequestDto request,
            String userEmail) {

        MultipartFile file = request.getFile();

        // 1. Check file empty
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Prescription file is required"
            );
        }

        // 2. Validate file type
        String contentType = file.getContentType();

        if (!"application/pdf".equalsIgnoreCase(contentType)
                && !"image/jpeg".equalsIgnoreCase(contentType)
                && !"image/png".equalsIgnoreCase(contentType)) {

            throw new IllegalArgumentException(
                    "Only PDF, JPG and PNG files are allowed"
            );
        }

        // 3. Create upload directory
        try {

            Path uploadPath =
                    Paths.get(uploadDir)
                            .toAbsolutePath()
                            .normalize();


            Files.createDirectories(uploadPath);

            // 4. Generate unique file name
            String originalFileName =
                    file.getOriginalFilename();

            String extension = "";

            if (originalFileName != null
                    && originalFileName.contains(".")) {

                extension =
                        originalFileName.substring(
                                originalFileName.lastIndexOf(".")
                        );
            }

            String fileName =
                    UUID.randomUUID() + extension;

            // 5. Complete file path
            Path filePath =
                    uploadPath.resolve(fileName);

            // 6. Save actual file
            Files.copy(
                    file.getInputStream(),
                    filePath
            );

            // 7. Create Prescription entity
            Prescription prescription =
                    new Prescription();

            prescription.setUserEmail(userEmail);

            prescription.setMedicineId(
                    request.getMedicineId()
            );

            prescription.setPrescriptionFileName(
                    fileName
            );

            prescription.setStatus(
                    PrescriptionStatus.PENDING
            );

            prescription.setUploadedAt(
                    LocalDateTime.now()
            );

            // 8. Save metadata in database
            Prescription savedPrescription =
                    prescriptionRepository.save(
                            prescription
                    );

            return mapToResponseDto(
                    savedPrescription
            );

        } catch (IOException e) {

            throw new IllegalArgumentException(
                    "Failed to upload prescription"
            );
        }
    }


    private PrescriptionResponseDto mapToResponseDto(
            Prescription prescription) {

        return new PrescriptionResponseDto(
                prescription.getId(),
                prescription.getUserEmail(),
                prescription.getMedicineId(),
                prescription.getPrescriptionFileName(),
                prescription.getStatus(),
                prescription.getRejectionReason(),
                prescription.getUploadedAt()
        );
    }
    // GET PENDING PRESCRIPTIONS - ADMIN
    public List<PrescriptionResponseDto> getPendingPrescriptions() {

        List<Prescription> prescriptions =
                prescriptionRepository.findByStatus(
                        PrescriptionStatus.PENDING
                );

        return prescriptions.stream()
                .map(this::mapToResponseDto)
                .toList();
    }
    // APPROVE PRESCRIPTION - ADMIN
    @Transactional
    public PrescriptionResponseDto approvePrescription(
            Long prescriptionId) {

        Prescription prescription =
                prescriptionRepository.findById(prescriptionId)
                        .orElseThrow(() ->
                                new PrescriptionNotFoundException(
                                        "Prescription not found with id: "
                                                + prescriptionId
                                ));

        if (prescription.getStatus()
                != PrescriptionStatus.PENDING) {

            throw new IllegalArgumentException(
                    "Only PENDING prescriptions can be approved"
            );
        }

        prescription.setStatus(
                PrescriptionStatus.APPROVED
        );

        Prescription updatedPrescription =
                prescriptionRepository.save(prescription);

        return mapToResponseDto(updatedPrescription);
    }
    // REJECT PRESCRIPTION - ADMIN
    @Transactional
    public PrescriptionResponseDto rejectPrescription(
            Long prescriptionId,
            String rejectionReason) {

        Prescription prescription =
                prescriptionRepository.findById(prescriptionId)
                        .orElseThrow(() ->
                                new PrescriptionNotFoundException(
                                        "Prescription not found with id: "
                                                + prescriptionId
                                ));

        if (prescription.getStatus()
                != PrescriptionStatus.PENDING) {

            throw new IllegalArgumentException(
                    "Only PENDING prescriptions can be rejected"
            );
        }

        prescription.setStatus(
                PrescriptionStatus.REJECTED
        );

        prescription.setRejectionReason(
                rejectionReason
        );

        Prescription updatedPrescription =
                prescriptionRepository.save(prescription);

        return mapToResponseDto(updatedPrescription);
    }
    public List<PrescriptionResponseDto> getMyPrescriptions(
            String userEmail) {

        List<Prescription> prescriptions =
                prescriptionRepository.findByUserEmail(userEmail);

        return prescriptions.stream()
                .map(this::mapToResponseDto)
                .toList();
    }
    public Resource getPrescriptionFile(
            Long prescriptionId,
            String userEmail) {

        Prescription prescription =
                prescriptionRepository.findById(prescriptionId)
                        .orElseThrow(() ->
                                new PrescriptionNotFoundException(
                                        "Prescription not found with id: "
                                                + prescriptionId
                                ));

        if (!prescription.getUserEmail().equals(userEmail)) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this prescription"
            );
        }

        Path filePath =
                Paths.get(uploadDir)
                        .resolve(
                                prescription.getPrescriptionFileName()
                        )
                        .normalize();

        Resource resource =
                new FileSystemResource(filePath);

        if (!resource.exists() || !resource.isReadable()) {

            throw new IllegalArgumentException(
                    "Prescription file not found"
            );
        }

        return resource;
    }

    public Resource getPrescriptionFile(
            Long prescriptionId,
            String userEmail,
            boolean isAdmin) {

        Prescription prescription =
                prescriptionRepository.findById(prescriptionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Prescription not found with id: "
                                                + prescriptionId
                                ));

        // USER → only own prescription
        if (!isAdmin &&
                !prescription.getUserEmail().equals(userEmail)) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this prescription"
            );
        }

        Path filePath =
                Paths.get(uploadDir)
                        .resolve(
                                prescription.getPrescriptionFileName()
                        )
                        .normalize();

        Resource resource =
                new FileSystemResource(filePath);

        if (!resource.exists() || !resource.isReadable()) {

            throw new IllegalArgumentException(
                    "Prescription file not found"
            );
        }

        return resource;
    }
    public PrescriptionResponseDto getMyPrescriptionById(
            Long prescriptionId,
            String userEmail) {

        Prescription prescription =
                prescriptionRepository.findById(prescriptionId)
                        .orElseThrow(() ->
                                new PrescriptionNotFoundException(
                                        "Prescription not found with id: "
                                                + prescriptionId
                                ));

        if (!prescription.getUserEmail().equals(userEmail)) {
            throw new IllegalArgumentException(
                    "You are not allowed to access this prescription"
            );
        }

        return mapToResponseDto(prescription);
    }


}