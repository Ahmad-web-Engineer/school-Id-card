package com.platform.idcard.agencyjob.controller;

import com.platform.idcard.agencyjob.dto.AgencyJobDetailResponse;
import com.platform.idcard.agencyjob.dto.AgencyJobSummaryResponse;
import com.platform.idcard.agencyjob.dto.RejectJobRequest;
import com.platform.idcard.agencyjob.service.AgencyJobService;
import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agency/jobs")
@PreAuthorize("hasRole('PRINTING_AGENCY')")
@Tag(name = "Agency Jobs", description = "Printing agency job queue, specs review, and acceptance/rejection")
public class AgencyJobController {

    private final AgencyJobService agencyJobService;

    public AgencyJobController(AgencyJobService agencyJobService) {
        this.agencyJobService = agencyJobService;
    }

    @GetMapping
    @Operation(summary = "List assigned jobs", description = "Paginated listing of jobs assigned to caller's agency")
    public ResponseEntity<ApiResponse<PagedResponse<AgencyJobSummaryResponse>>> listJobs(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        PagedResponse<AgencyJobSummaryResponse> response = agencyJobService.getAgencyJobs(
                agencyId, pageable, status, search
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job details", description = "Retrieves technical printing specs and job item quantities")
    public ResponseEntity<ApiResponse<AgencyJobDetailResponse>> getJobDetails(
            @PathVariable UUID id
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        AgencyJobDetailResponse response = agencyJobService.getAgencyJobById(id, agencyId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept assigned job", description = "Transitions job to AGENCY_ACCEPTED and commits delivery date")
    public ResponseEntity<ApiResponse<AgencyJobDetailResponse>> acceptJob(
            @PathVariable UUID id
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        UUID userId = SecurityUtils.getCurrentUserId();
        AgencyJobDetailResponse response = agencyJobService.acceptJob(id, agencyId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject assigned job", description = "Transitions job to REJECTED with mandatory reason")
    public ResponseEntity<ApiResponse<AgencyJobDetailResponse>> rejectJob(
            @PathVariable UUID id,
            @Valid @RequestBody RejectJobRequest request
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        UUID userId = SecurityUtils.getCurrentUserId();
        AgencyJobDetailResponse response = agencyJobService.rejectJob(id, agencyId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/print-pdf")
    @Operation(summary = "Download print-ready cards PDF", description = "Generates high-resolution multi-page PDF with student cards for printing")
    public ResponseEntity<byte[]> downloadJobPrintPdf(@PathVariable UUID id) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        byte[] pdfBytes = agencyJobService.renderJobCardsPdf(id, agencyId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"job_cards_" + id + ".pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/{id}/students/{studentId}/print-pdf")
    @Operation(summary = "Download single student ID card PDF", description = "Generates print-ready 2-page PDF for a specific student")
    public ResponseEntity<byte[]> downloadJobStudentPrintPdf(@PathVariable UUID id, @PathVariable UUID studentId) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        byte[] pdfBytes = agencyJobService.renderJobSingleStudentPdf(id, studentId, agencyId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"card_" + studentId + ".pdf\"")
                .body(pdfBytes);
    }

    @GetMapping("/{id}/print-a4-sheet")
    @Operation(summary = "Download A4 imposition sheet PDF", description = "Generates multi-page A4 imposition sheet PDF with duplex column mirroring for production printing")
    public ResponseEntity<byte[]> downloadJobA4SheetPdf(@PathVariable UUID id) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        AgencyJobDetailResponse detail = agencyJobService.getAgencyJobById(id, agencyId);
        byte[] pdfBytes = agencyJobService.renderJobA4SheetPdf(id, agencyId);
        String filename = "job_" + detail.jobNumber() + "_a4_sheet.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdfBytes);
    }

    @GetMapping("/{id}/print-a4-image")
    @Operation(summary = "Download A4 imposition sheet JPEG", description = "Generates high-resolution 300 DPI A4 imposition sheet JPEG image with crop marks and duplex layout")
    public ResponseEntity<byte[]> downloadJobA4SheetImage(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "front") String side
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        AgencyJobDetailResponse detail = agencyJobService.getAgencyJobById(id, agencyId);
        byte[] imageBytes = agencyJobService.renderJobA4SheetImage(id, agencyId, side);
        String filename = "job_" + detail.jobNumber() + "_a4_" + (side != null ? side.toLowerCase() : "front") + ".jpg";
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(imageBytes);
    }

    @GetMapping("/{id}/download-images")
    @Operation(summary = "Download high-resolution JPEG images ZIP", description = "Exports 300 DPI JPEG cards in a zip archive")
    public ResponseEntity<byte[]> downloadJobImagesZip(@PathVariable UUID id) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        AgencyJobDetailResponse detail = agencyJobService.getAgencyJobById(id, agencyId);
        byte[] zipBytes = agencyJobService.renderJobImagesZip(id, agencyId);
        String filename = "job_" + detail.jobNumber() + "_cards_jpeg.zip";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(zipBytes);
    }

    @GetMapping("/{id}/students/{studentId}/download-image")
    @Operation(summary = "Download single student card JPEG", description = "Exports single student card side as 300 DPI JPEG")
    public ResponseEntity<byte[]> downloadJobStudentImage(
            @PathVariable UUID id,
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "front") String side
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        byte[] imageBytes = agencyJobService.renderJobSingleStudentImage(id, studentId, agencyId, side);
        String filename = "card_" + studentId + "_" + (side != null ? side.toLowerCase() : "front") + ".jpg";
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(imageBytes);
    }
}
