package com.platform.idcard.dashboard.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.dashboard.dto.AgencyDashboardStatsResponse;
import com.platform.idcard.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agency/dashboard")
@PreAuthorize("hasRole('PRINTING_AGENCY')")
@Tag(name = "Agency Dashboard", description = "Live agency KPIs, assigned print jobs, and fulfillment metrics")
public class AgencyDashboardController {

    private final DashboardService dashboardService;

    public AgencyDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get agency dashboard stats", description = "Returns real-time job queues, capacity, and earnings for logged-in agency")
    public ResponseEntity<ApiResponse<AgencyDashboardStatsResponse>> getStats() {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getAgencyDashboardStats(agencyId)));
    }
}
