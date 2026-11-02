package com.platform.idcard.dashboard.controller;

import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.dashboard.dto.AdminDashboardStatsResponse;
import com.platform.idcard.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Dashboard", description = "Live platform metrics, revenue, and active orders for Super Admin")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get admin dashboard stats", description = "Returns real-time platform KPIs, revenue, and recent orders")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getAdminDashboardStats()));
    }
}
