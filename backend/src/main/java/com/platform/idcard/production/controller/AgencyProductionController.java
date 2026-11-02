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
@RequestMapping("/api/v1/agency")
@PreAuthorize("hasRole('PRINTING_AGENCY')")
@Tag(name = "Agency Production & QC", description = "Printing agency batch management, production progress, and quality checks")
public class AgencyProductionController {

    private final ProductionService productionService;

    public AgencyProductionController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @PostMapping("/jobs/{id}/start")
    @Operation(summary = "Start production", description = "Transitions accepted job into IN_PRODUCTION status")
    public ResponseEntity<ApiResponse<StartProductionResponse>> startProduction(@PathVariable UUID id) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        UUID userId = SecurityUtils.getCurrentUserId();
        StartProductionResponse response = productionService.startProduction(id, agencyId, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/jobs/{id}/batches")
    @Operation(summary = "Create production batch", description = "Creates a planned sub-batch for an assignment")
    public ResponseEntity<ApiResponse<AgencyProductionBatchResponse>> createBatch(
            @PathVariable UUID id,
            @Valid @RequestBody CreateBatchRequest request
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        UUID userId = SecurityUtils.getCurrentUserId();
        AgencyProductionBatchResponse response = productionService.createBatch(id, agencyId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/jobs/{id}/batches")
    @Operation(summary = "List batches for job", description = "Paginated listing of production batches for caller's job")
    public ResponseEntity<ApiResponse<PagedResponse<AgencyProductionBatchResponse>>> listBatches(
            @PathVariable UUID id,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        PagedResponse<AgencyProductionBatchResponse> response = productionService.getAgencyBatches(id, agencyId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/batches/{batchId}/updates")
    @Operation(summary = "Post production update", description = "Records printed and rejected quantities for a batch")
    public ResponseEntity<ApiResponse<AgencyProductionBatchResponse>> addProductionUpdate(
            @PathVariable UUID batchId,
            @Valid @RequestBody ProductionUpdateRequest request
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        UUID userId = SecurityUtils.getCurrentUserId();
        AgencyProductionBatchResponse response = productionService.addProductionUpdate(batchId, agencyId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/jobs/{id}/quality-checks")
    @Operation(summary = "Submit quality check", description = "Submits inspection report; auto-triggers reprint request if rejected")
    public ResponseEntity<ApiResponse<QualityCheckResponse>> submitQualityCheck(
            @PathVariable UUID id,
            @Valid @RequestBody QualityCheckRequest request
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        UUID userId = SecurityUtils.getCurrentUserId();
        QualityCheckResponse response = productionService.submitQualityCheck(id, agencyId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/jobs/{id}/quality-checks")
    @Operation(summary = "List quality checks", description = "Paginated quality check inspection history for caller's job")
    public ResponseEntity<ApiResponse<PagedResponse<QualityCheckResponse>>> listQualityChecks(
            @PathVariable UUID id,
            @PageableDefault(size = 10, sort = "inspectedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        PagedResponse<QualityCheckResponse> response = productionService.getAgencyQualityChecks(id, agencyId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
