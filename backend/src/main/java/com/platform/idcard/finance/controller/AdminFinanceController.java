package com.platform.idcard.finance.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.finance.dto.*;
import com.platform.idcard.finance.service.FinanceService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/finance")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Finance & P&L", description = "Global revenue accounting, school invoice management, manual payments, and agency settlements")
public class AdminFinanceController {

    private final FinanceService financeService;

    public AdminFinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Platform financial summary", description = "Gross revenue, agency costs, platform margins, and receivables")
    public ResponseEntity<ApiResponse<AdminFinanceSummaryResponse>> getSummary() {
        AdminFinanceSummaryResponse response = financeService.getAdminFinanceSummary();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List global invoices", description = "Paginated list of all invoices across schools")
    public ResponseEntity<ApiResponse<PagedResponse<AdminInvoiceResponse>>> listInvoices(
            @PageableDefault(size = 10, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) String status
    ) {
        PagedResponse<AdminInvoiceResponse> response = financeService.getAdminInvoices(pageable, schoolId, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/orders/{id}/invoices")
    @Operation(summary = "Generate order invoice", description = "Generates official tax invoice for an order with an approved quotation")
    public ResponseEntity<ApiResponse<AdminInvoiceResponse>> generateInvoice(
            @PathVariable UUID id,
            @RequestBody(required = false) GenerateInvoiceRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminInvoiceResponse response = financeService.generateInvoice(id, adminUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/payments")
    @Operation(summary = "List global payments", description = "Paginated list of payments received across schools")
    public ResponseEntity<ApiResponse<PagedResponse<AdminPaymentResponse>>> listPayments(
            @PageableDefault(size = 10, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) String status
    ) {
        PagedResponse<AdminPaymentResponse> response = financeService.getAdminPayments(pageable, schoolId, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/orders/{id}/payments")
    @Operation(summary = "Record offline/manual payment", description = "Records a payment received via offline bank transfer, cheque, or cash")
    public ResponseEntity<ApiResponse<AdminPaymentResponse>> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminPaymentResponse response = financeService.adminRecordPayment(id, adminUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/settlements")
    @Operation(summary = "List agency settlements", description = "Paginated list of printing agency settlements")
    public ResponseEntity<ApiResponse<PagedResponse<AdminSettlementResponse>>> listSettlements(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID agencyId,
            @RequestParam(required = false) String status
    ) {
        PagedResponse<AdminSettlementResponse> response = financeService.getAdminSettlements(pageable, agencyId, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/assignments/{id}/settlement")
    @Operation(summary = "Generate agency settlement", description = "Calculates agreed payout, applies defect deductions, and generates settlement statement")
    public ResponseEntity<ApiResponse<AdminSettlementResponse>> generateSettlement(
            @PathVariable UUID id,
            @RequestBody(required = false) GenerateSettlementRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminSettlementResponse response = financeService.generateAgencySettlement(id, adminUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/settlements/{id}/pay")
    @Operation(summary = "Pay agency settlement", description = "Records a settlement payout transaction to a printing agency")
    public ResponseEntity<ApiResponse<AdminSettlementResponse>> paySettlement(
            @PathVariable UUID id,
            @Valid @RequestBody PaySettlementRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminSettlementResponse response = financeService.payAgencySettlement(id, adminUserId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
