package com.platform.idcard.template.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.template.dto.*;
import com.platform.idcard.template.service.CardTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/templates")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin ID Card Templates", description = "Admin ID card template designer, versioning, and assignment")
public class AdminCardTemplateController {

    private final CardTemplateService cardTemplateService;

    public AdminCardTemplateController(CardTemplateService cardTemplateService) {
        this.cardTemplateService = cardTemplateService;
    }

    @GetMapping
    @Operation(summary = "List card templates", description = "Paginated list of all ID card templates with status & version metadata")
    public ResponseEntity<PagedResponse<CardTemplateResponse>> listTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(cardTemplateService.getTemplates(page, size, search, category, status));
    }

    @PostMapping
    @Operation(summary = "Create template master", description = "Creates a new ID card template and initial Version 1 in DRAFT status")
    public ResponseEntity<ApiResponse<CardTemplateDetailResponse>> createTemplate(
            @Valid @RequestBody CreateCardTemplateRequest request
    ) {
        CardTemplateDetailResponse response = cardTemplateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID", description = "Retrieves card template details along with complete version history")
    public ResponseEntity<ApiResponse<CardTemplateDetailResponse>> getTemplate(@PathVariable UUID id) {
        CardTemplateDetailResponse response = cardTemplateService.getTemplateById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/versions")
    @Operation(summary = "Save new template version", description = "Creates an incremented version in DRAFT status for canvas modifications")
    public ResponseEntity<ApiResponse<TemplateVersionResponse>> createVersion(
            @PathVariable UUID id,
            @Valid @RequestBody CreateTemplateVersionRequest request
    ) {
        TemplateVersionResponse response = cardTemplateService.createVersion(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/versions/{versionId}/submit")
    @Operation(summary = "Submit version for approval", description = "Transitions a DRAFT version into PENDING_APPROVAL")
    public ResponseEntity<ApiResponse<TemplateVersionResponse>> submitVersion(
            @PathVariable UUID id,
            @PathVariable UUID versionId
    ) {
        TemplateVersionResponse response = cardTemplateService.submitVersion(id, versionId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/versions/{versionId}/approve")
    @Operation(summary = "Approve and lock template version", description = "Transitions version to APPROVED and archives previously approved versions")
    public ResponseEntity<ApiResponse<TemplateVersionResponse>> approveVersion(
            @PathVariable UUID id,
            @PathVariable UUID versionId
    ) {
        UUID approverUserId = SecurityUtils.getCurrentUserId();
        TemplateVersionResponse response = cardTemplateService.approveVersion(id, versionId, approverUserId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}/versions/{versionId}/preview")
    @Operation(summary = "Render template version PDF preview", description = "Generates high-resolution 2-page PDF preview with sample student data")
    public ResponseEntity<byte[]> previewVersion(
            @PathVariable UUID id,
            @PathVariable UUID versionId
    ) {
        byte[] pdfBytes = cardTemplateService.renderPreviewPdf(id, versionId, null, null);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"template_preview_v" + versionId + ".pdf\"")
                .body(pdfBytes);
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign template to school", description = "Assigns an approved template to an educational institution for an academic session")
    public ResponseEntity<ApiResponse<TemplateAssignmentResponse>> assignTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTemplateRequest request
    ) {
        TemplateAssignmentResponse response = cardTemplateService.assignTemplateToSchool(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
