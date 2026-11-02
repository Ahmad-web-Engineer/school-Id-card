package com.platform.idcard.organization.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.organization.dto.AgencyResponse;
import com.platform.idcard.organization.service.AgencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agency/profile")
@PreAuthorize("hasRole('PRINTING_AGENCY')")
@Tag(name = "Agency Profile", description = "Printing agency self-profile access")
public class AgencyProfileController {

    private final AgencyService agencyService;

    public AgencyProfileController(AgencyService agencyService) {
        this.agencyService = agencyService;
    }

    @GetMapping
    @Operation(summary = "Get own agency profile", description = "Returns agency details derived strictly from logged-in token")
    public ResponseEntity<ApiResponse<AgencyResponse>> getOwnProfile() {
        // Non-negotiable rule 2: Read agency_id strictly from authenticated session token
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        AgencyResponse response = agencyService.getAgencyById(agencyId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
