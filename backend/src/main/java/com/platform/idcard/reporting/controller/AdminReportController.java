package com.platform.idcard.reporting.controller;

import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.reporting.dto.AdminFinancialReportResponse;
import com.platform.idcard.reporting.dto.AdminOperationalReportResponse;
import com.platform.idcard.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Reports", description = "Global operational, production, and financial analytics")
public class AdminReportController {

    private final ReportingService reportingService;

    public AdminReportController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/operational")
    @Operation(summary = "Operational report", description = "Global order statuses, printed cards, delivery throughput, and QC pass rate")
    public ResponseEntity<ApiResponse<AdminOperationalReportResponse>> getOperationalReport() {
        AdminOperationalReportResponse response = reportingService.getAdminOperationalReport();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/financial")
    @Operation(summary = "Financial report", description = "Platform gross revenue, direct agency cost, margins, and receivables")
    public ResponseEntity<ApiResponse<AdminFinancialReportResponse>> getFinancialReport() {
        AdminFinancialReportResponse response = reportingService.getAdminFinancialReport();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
