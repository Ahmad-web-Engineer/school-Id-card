package com.platform.idcard.finance.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.finance.dto.AgencyFinanceSummaryResponse;
import com.platform.idcard.finance.dto.AgencySettlementResponse;
import com.platform.idcard.finance.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agency/finance")
@PreAuthorize("hasRole('PRINTING_AGENCY')")
@Tag(name = "Agency Finance", description = "Agency agreed payouts, defect deductions, and settlement history")
public class AgencyFinanceController {

    private final FinanceService financeService;

    public AgencyFinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Agency financial summary", description = "Agreed payout totals, reprint defect deductions, and pending payouts")
    public ResponseEntity<ApiResponse<AgencyFinanceSummaryResponse>> getSummary() {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        AgencyFinanceSummaryResponse response = financeService.getAgencyFinanceSummary(agencyId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/settlements")
    @Operation(summary = "List settlements", description = "Paginated list of job settlements and payment transactions")
    public ResponseEntity<ApiResponse<PagedResponse<AgencySettlementResponse>>> listSettlements(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        PagedResponse<AgencySettlementResponse> response = financeService.getAgencySettlements(agencyId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/settlements/{id}")
    @Operation(summary = "Get settlement details", description = "Detailed breakdown of a specific settlement statement")
    public ResponseEntity<ApiResponse<AgencySettlementResponse>> getSettlement(@PathVariable UUID id) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        AgencySettlementResponse response = financeService.getAgencySettlementById(id, agencyId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
