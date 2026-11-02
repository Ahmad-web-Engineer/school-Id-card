package com.platform.idcard.agencyjob.controller;

import com.platform.idcard.agencyjob.dto.AdminAgencyAssignmentDetailResponse;
import com.platform.idcard.agencyjob.dto.AdminAgencyAssignmentResponse;
import com.platform.idcard.agencyjob.dto.AssignOrderRequest;
import com.platform.idcard.agencyjob.dto.ReassignJobRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Agency Assignments", description = "Job assignment, splitting, and reassignments for printing agencies")
public class AdminAgencyAssignmentController {

    private final AgencyJobService agencyJobService;

    public AdminAgencyAssignmentController(AgencyJobService agencyJobService) {
        this.agencyJobService = agencyJobService;
    }

    @PostMapping("/orders/{id}/assignments")
    @Operation(summary = "Assign order to agencies", description = "Allocates order quantities to one or more agencies")
    public ResponseEntity<ApiResponse<List<AdminAgencyAssignmentResponse>>> assignOrder(
            @PathVariable UUID id,
            @Valid @RequestBody AssignOrderRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        List<AdminAgencyAssignmentResponse> response = agencyJobService.assignOrderToAgencies(id, adminUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/assignments")
    @Operation(summary = "List all assignments", description = "Paginated listing of all agency assignments")
    public ResponseEntity<ApiResponse<PagedResponse<AdminAgencyAssignmentResponse>>> listAssignments(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) UUID agencyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        PagedResponse<AdminAgencyAssignmentResponse> response = agencyJobService.getAdminAssignments(
                pageable, orderId, agencyId, status, search
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "Get assignment details", description = "Retrieves detailed agency assignment information")
    public ResponseEntity<ApiResponse<AdminAgencyAssignmentDetailResponse>> getAssignmentDetails(
            @PathVariable UUID id
    ) {
        AdminAgencyAssignmentDetailResponse response = agencyJobService.getAdminAssignmentById(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/assignments/{id}/reassign")
    @Operation(summary = "Reassign rejected or pending job", description = "Reallocates job to another agency")
    public ResponseEntity<ApiResponse<AdminAgencyAssignmentResponse>> reassignJob(
            @PathVariable UUID id,
            @Valid @RequestBody ReassignJobRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminAgencyAssignmentResponse response = agencyJobService.reassignJob(id, adminUserId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
