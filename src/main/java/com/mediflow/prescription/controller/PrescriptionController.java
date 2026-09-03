package com.mediflow.prescription.controller;

import com.mediflow.prescription.dto.PrescriptionRejectRequestDto;
import com.mediflow.prescription.dto.PrescriptionRequestDto;
import com.mediflow.prescription.dto.PrescriptionResponseDto;
import com.mediflow.prescription.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
 // @RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService){
        this.prescriptionService=prescriptionService;
    }


    // USER + ADMIN → Upload prescription
    // Why: Authentication থেকে logged-in user's email নেওয়া হচ্ছে,
    // যাতে prescription অন্য user's নামে upload না হয়।
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PrescriptionResponseDto> uploadPrescription(
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


    // ADMIN → Get all pending prescriptions
    // Why: শুধুমাত্র ADMIN pending prescription review করতে পারবে।
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<PrescriptionResponseDto>>
    getPendingPrescriptions() {

        List<PrescriptionResponseDto> prescriptions =
                prescriptionService.getPendingPrescriptions();

        return ResponseEntity.ok(prescriptions);
    }


    // ADMIN → Approve prescription
    // Why: শুধু ADMIN prescription approve করতে পারবে।
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<PrescriptionResponseDto>
    approvePrescription(
            @PathVariable Long id,
            Authentication authentication) {

        String adminEmail = authentication.getName();

        PrescriptionResponseDto response =
                prescriptionService.approvePrescription(
                        id,
                        adminEmail
                );

        return ResponseEntity.ok(response);
    }


    // ADMIN → Reject prescription
    // Why: Rejection reason সহ admin-এর email service-এ পাঠানো হচ্ছে,
    // যাতে audit log-এ কে reject করেছে সেটা track করা যায়।
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<PrescriptionResponseDto>
    rejectPrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionRejectRequestDto request,
            Authentication authentication) {

        String adminEmail = authentication.getName();

        PrescriptionResponseDto response =
                prescriptionService.rejectPrescription(
                        id,
                        request.getRejectionReason(),
                        adminEmail
                );

        return ResponseEntity.ok(response);
    }


    // USER → Get own prescriptions
    // Why: Authentication থেকে user's email নিয়ে
    // শুধু সেই user's prescriptions ফেরত দেওয়া হচ্ছে।
    @GetMapping("/my-prescriptions")
    public ResponseEntity<List<PrescriptionResponseDto>>
    getMyPrescriptions(
            Authentication authentication) {

        String userEmail = authentication.getName();

        List<PrescriptionResponseDto> prescriptions =
                prescriptionService.getMyPrescriptions(
                        userEmail
                );

        return ResponseEntity.ok(prescriptions);
    }


    // USER + ADMIN → Get prescription file
    // Why: ADMIN সব prescription file access করতে পারবে,
    // কিন্তু USER শুধুমাত্র নিজের file access করতে পারবে।
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
                                        .equals("ROLE_ADMIN")
                        );

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
                                resource.getFilename() +
                                "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }


    // USER → Get own prescription by ID
    // Why: Service layer ownership check করবে,
    // তাই user অন্য user's prescription দেখতে পারবে না।
    @GetMapping("/my-prescriptions/{id}")
    public ResponseEntity<PrescriptionResponseDto>
    getMyPrescriptionById(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = authentication.getName();

        PrescriptionResponseDto response =
                prescriptionService.getMyPrescriptionById(
                        id,
                        userEmail
                );

        return ResponseEntity.ok(response);
    }
}