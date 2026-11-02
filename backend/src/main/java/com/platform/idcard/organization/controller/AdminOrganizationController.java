package com.platform.idcard.organization.controller;

import com.platform.idcard.auth.dto.UserSummaryResponse;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.organization.dto.*;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Organizations", description = "Admin management of schools, agencies and user accounts")
public class AdminOrganizationController {

    private final SchoolService schoolService;
    private final AgencyService agencyService;

    public AdminOrganizationController(SchoolService schoolService, AgencyService agencyService) {
        this.schoolService = schoolService;
        this.agencyService = agencyService;
    }

    // --- Schools ---

    @GetMapping("/schools")
    @Operation(summary = "List schools", description = "Paginated list of registered schools with search filter")
    public ResponseEntity<PagedResponse<SchoolResponse>> listSchools(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(schoolService.getSchools(page, size, search, status));
    }

    @PostMapping("/schools")
    @Operation(summary = "Register school", description = "Registers a new school in the platform")
    public ResponseEntity<ApiResponse<SchoolResponse>> createSchool(@Valid @RequestBody SchoolRequest request) {
        SchoolResponse response = schoolService.createSchool(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/schools/{id}")
    @Operation(summary = "Get school by ID", description = "Fetches detailed school profile")
    public ResponseEntity<ApiResponse<SchoolResponse>> getSchool(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(schoolService.getSchoolById(id)));
    }

    @PutMapping("/schools/{id}")
    @Operation(summary = "Update school", description = "Updates an existing school profile")
    public ResponseEntity<ApiResponse<SchoolResponse>> updateSchool(
            @PathVariable UUID id,
            @Valid @RequestBody SchoolRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(schoolService.updateSchool(id, request)));
    }

    @PostMapping("/schools/{id}/users")
    @Operation(summary = "Create school user", description = "Creates a login user account assigned to the school")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> createSchoolUser(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOrganizationUserRequest request
    ) {
        UserSummaryResponse response = schoolService.createSchoolUser(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    // --- Agencies ---

    @GetMapping("/agencies")
    @Operation(summary = "List agencies", description = "Paginated list of printing agencies with search filter")
    public ResponseEntity<PagedResponse<AgencyResponse>> listAgencies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(agencyService.getAgencies(page, size, search, status));
    }

    @PostMapping("/agencies")
    @Operation(summary = "Register agency", description = "Registers a new printing agency partner")
    public ResponseEntity<ApiResponse<AgencyResponse>> createAgency(@Valid @RequestBody AgencyRequest request) {
        AgencyResponse response = agencyService.createAgency(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/agencies/{id}")
    @Operation(summary = "Get agency by ID", description = "Fetches detailed printing agency profile")
    public ResponseEntity<ApiResponse<AgencyResponse>> getAgency(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(agencyService.getAgencyById(id)));
    }

    @PutMapping("/agencies/{id}")
    @Operation(summary = "Update agency", description = "Updates an existing printing agency profile")
    public ResponseEntity<ApiResponse<AgencyResponse>> updateAgency(
            @PathVariable UUID id,
            @Valid @RequestBody AgencyRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(agencyService.updateAgency(id, request)));
    }

    @PostMapping("/agencies/{id}/users")
    @Operation(summary = "Create agency user", description = "Creates a login user account assigned to the agency")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> createAgencyUser(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOrganizationUserRequest request
    ) {
        UserSummaryResponse response = agencyService.createAgencyUser(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
