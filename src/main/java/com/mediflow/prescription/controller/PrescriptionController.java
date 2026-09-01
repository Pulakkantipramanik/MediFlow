package com.mediflow.prescription.controller;

import com.mediflow.prescription.dto.PrescriptionRejectRequestDto;
import com.mediflow.prescription.dto.PrescriptionRequestDto;
import com.mediflow.prescription.dto.PrescriptionResponseDto;
import com.mediflow.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    // USER + ADMIN → Upload
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PrescriptionResponseDto>
    uploadPrescription(
            @Valid @ModelAttribute PrescriptionRequestDto request,
            Authentication authentication) {

        String userEmail = authentication.getName();

        PrescriptionResponseDto response =
                prescriptionService.uploadPrescription(
                        request,
                        userEmail
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ADMIN → Pending prescriptions
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<PrescriptionResponseDto>>
    getPendingPrescriptions() {

        List<PrescriptionResponseDto> prescriptions =
                prescriptionService.getPendingPrescriptions();

        return ResponseEntity.ok(prescriptions);
    }


    // ADMIN → Approve
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<PrescriptionResponseDto>
    approvePrescription(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                prescriptionService.approvePrescription(id)
        );
    }


    // ADMIN → Reject
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<PrescriptionResponseDto>
    rejectPrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionRejectRequestDto request) {

        return ResponseEntity.ok(
                prescriptionService.rejectPrescription(
                        id,
                        request.getRejectionReason()
                )
        );
    }

    @GetMapping("/my-prescriptions")
    public ResponseEntity<List<PrescriptionResponseDto>>
    getMyPrescriptions(
            Authentication authentication) {

        String userEmail = authentication.getName();

        List<PrescriptionResponseDto> prescriptions =
                prescriptionService.getMyPrescriptions(userEmail);

        return ResponseEntity.ok(prescriptions);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getPrescriptionFile(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();

        boolean isAdmin =
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_ADMIN"));

        Resource resource =
                prescriptionService.getPrescriptionFile(
                        id,
                        userEmail,
                        isAdmin
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" +
                                resource.getFilename() + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
    @GetMapping("/my-prescriptions/{id}")
    public ResponseEntity<PrescriptionResponseDto>
    getMyPrescriptionById(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();

        return ResponseEntity.ok(
                prescriptionService.getMyPrescriptionById(
                        id,
                        userEmail
                )
        );
    }

}