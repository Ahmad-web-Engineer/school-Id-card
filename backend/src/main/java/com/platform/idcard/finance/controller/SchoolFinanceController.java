package com.platform.idcard.finance.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.finance.dto.RecordPaymentRequest;
import com.platform.idcard.finance.dto.SchoolFinanceSummaryResponse;
import com.platform.idcard.finance.dto.SchoolInvoiceResponse;
import com.platform.idcard.finance.dto.SchoolPaymentResponse;
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
@RequestMapping("/api/v1/school/finance")
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "School Finance", description = "School invoices, payment processing, and accounting balance")
public class SchoolFinanceController {

    private final FinanceService financeService;

    public SchoolFinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Financial summary", description = "Returns total billed, total paid, and outstanding balance for the logged in school")
    public ResponseEntity<ApiResponse<SchoolFinanceSummaryResponse>> getSummary() {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        SchoolFinanceSummaryResponse response = financeService.getSchoolFinanceSummary(schoolId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List invoices", description = "Paginated list of tax invoices issued to this school")
    public ResponseEntity<ApiResponse<PagedResponse<SchoolInvoiceResponse>>> listInvoices(
            @PageableDefault(size = 10, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        PagedResponse<SchoolInvoiceResponse> response = financeService.getSchoolInvoices(schoolId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "Get invoice details", description = "Detailed breakdown of a specific invoice")
    public ResponseEntity<ApiResponse<SchoolInvoiceResponse>> getInvoice(@PathVariable UUID id) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        SchoolInvoiceResponse response = financeService.getSchoolInvoiceById(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/payments")
    @Operation(summary = "List payments", description = "Paginated history of all payments made by this school")
    public ResponseEntity<ApiResponse<PagedResponse<SchoolPaymentResponse>>> listPayments(
            @PageableDefault(size = 10, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        PagedResponse<SchoolPaymentResponse> response = financeService.getSchoolPayments(schoolId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/orders/{id}/payments")
    @Operation(summary = "Record school payment", description = "School records a payment (advance or milestone) for their order")
    public ResponseEntity<ApiResponse<SchoolPaymentResponse>> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest request
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        UUID userId = SecurityUtils.getCurrentUserId();
        SchoolPaymentResponse response = financeService.recordSchoolPayment(id, schoolId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
