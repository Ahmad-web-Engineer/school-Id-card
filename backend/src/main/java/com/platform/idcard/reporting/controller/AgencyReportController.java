package com.platform.idcard.reporting.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.reporting.dto.AgencyPerformanceReportResponse;
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
@RequestMapping("/api/v1/agency/reports")
@PreAuthorize("hasRole('PRINTING_AGENCY')")
@Tag(name = "Agency Reports", description = "Printing agency production metrics, quality rates, and payout ledger")
public class AgencyReportController {

    private final ReportingService reportingService;

    public AgencyReportController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/performance")
    @Operation(summary = "Agency performance report", description = "Throughput, QC pass rate, reprint defects, and earnings summary")
    public ResponseEntity<ApiResponse<AgencyPerformanceReportResponse>> getPerformance() {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        AgencyPerformanceReportResponse response = reportingService.getAgencyPerformanceReport(agencyId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
