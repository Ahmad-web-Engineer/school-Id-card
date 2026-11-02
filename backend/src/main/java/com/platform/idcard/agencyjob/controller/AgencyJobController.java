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
}
