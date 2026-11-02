package com.platform.idcard.dashboard.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.dashboard.dto.SchoolDashboardStatsResponse;
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
@RequestMapping("/api/v1/school/dashboard")
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "School Dashboard", description = "Live school KPIs, roster stats, and order tracking for School Admin")
public class SchoolDashboardController {

    private final DashboardService dashboardService;

    public SchoolDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get school dashboard stats", description = "Returns real-time student count, approved templates, and orders for logged-in school")
    public ResponseEntity<ApiResponse<SchoolDashboardStatsResponse>> getStats() {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getSchoolDashboardStats(schoolId)));
    }
}
