package com.platform.idcard.production.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.production.dto.*;
import com.platform.idcard.production.service.ProductionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Production & Quality Control", description = "Global production batch monitoring, QC inspections, and reprint request approvals")
public class AdminProductionController {

    private final ProductionService productionService;

    public AdminProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping("/production/batches")
    @Operation(summary = "List all production batches", description = "Paginated listing of all batches across printing agencies")
    public ResponseEntity<ApiResponse<PagedResponse<AdminProductionBatchResponse>>> listBatches(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID assignmentId,
            @RequestParam(required = false) UUID agencyId,
            @RequestParam(required = false) String status
    ) {
        PagedResponse<AdminProductionBatchResponse> response = productionService.getAdminBatches(
                pageable, assignmentId, agencyId, status
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/production/quality-checks")
    @Operation(summary = "List all quality checks", description = "Paginated inspection reports across assignments")
    public ResponseEntity<ApiResponse<PagedResponse<AdminQualityCheckResponse>>> listQualityChecks(
            @PageableDefault(size = 10, sort = "inspectedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID assignmentId,
            @RequestParam(required = false) UUID agencyId,
            @RequestParam(required = false) String status
    ) {
        PagedResponse<AdminQualityCheckResponse> response = productionService.getAdminQualityChecks(
                pageable, assignmentId, agencyId, status
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/assignments/{id}/quality-checks")
    @Operation(summary = "Perform admin quality check", description = "Admin quality inspection (for Model B delivery or audits)")
    public ResponseEntity<ApiResponse<AdminQualityCheckResponse>> performAdminQualityCheck(
            @PathVariable UUID id,
            @Valid @RequestBody QualityCheckRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminQualityCheckResponse response = productionService.performAdminQualityCheck(id, adminUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/production/reprints")
    @Operation(summary = "List reprint requests", description = "Paginated list of automated and manual reprint requests")
    public ResponseEntity<ApiResponse<PagedResponse<AdminReprintRequestResponse>>> listReprintRequests(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID assignmentId,
            @RequestParam(required = false) String status
    ) {
        PagedResponse<AdminReprintRequestResponse> response = productionService.getAdminReprintRequests(
                pageable, assignmentId, status
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/reprints/{id}/approve")
    @Operation(summary = "Approve reprint request", description = "Approves replacement cards and assigns cost bearer liability")
    public ResponseEntity<ApiResponse<AdminReprintRequestResponse>> approveReprint(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveReprintRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminReprintRequestResponse response = productionService.approveReprintRequest(id, adminUserId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/reprints/{id}/reject")
    @Operation(summary = "Reject reprint request", description = "Rejects reprint request with mandatory explanation")
    public ResponseEntity<ApiResponse<AdminReprintRequestResponse>> rejectReprint(
            @PathVariable UUID id,
            @Valid @RequestBody RejectReprintRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminReprintRequestResponse response = productionService.rejectReprintRequest(id, adminUserId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
