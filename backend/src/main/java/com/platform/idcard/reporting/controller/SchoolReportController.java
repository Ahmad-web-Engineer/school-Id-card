package com.platform.idcard.reporting.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.reporting.dto.SchoolReportSummaryResponse;
import com.platform.idcard.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/reports")
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "School Reports", description = "School order progress, student card delivery status, and financial balances")
public class SchoolReportController {

    private final ReportingService reportingService;

    public SchoolReportController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/summary")
    @Operation(summary = "School reporting summary", description = "Consolidated summary of orders, cards ordered/delivered, and billing")
    public ResponseEntity<ApiResponse<SchoolReportSummaryResponse>> getSummary() {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        SchoolReportSummaryResponse response = reportingService.getSchoolReportSummary(schoolId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
