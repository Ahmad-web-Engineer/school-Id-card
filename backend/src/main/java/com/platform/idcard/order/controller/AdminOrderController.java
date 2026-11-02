package com.platform.idcard.order.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.order.dto.*;
import com.platform.idcard.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Orders", description = "Admin order verification, review, corrections, and quotation generation")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List all orders", description = "Paginated list of all school orders with filters")
    public ResponseEntity<PagedResponse<AdminOrderSummaryResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(orderService.getAdminOrders(page, size, schoolId, search, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details", description = "Retrieves full order details including student snapshots and quotation")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAdminOrderById(id)));
    }

    @PostMapping("/{id}/start-review")
    @Operation(summary = "Start order review", description = "Transitions order from SUBMITTED to UNDER_REVIEW")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> startReview(@PathVariable UUID id) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.startReview(id, adminUserId)));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify order", description = "Verifies student data and transitions order to VERIFIED")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> verifyOrder(@PathVariable UUID id) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.verifyOrder(id, adminUserId)));
    }

    @PostMapping("/{id}/corrections")
    @Operation(summary = "Request correction", description = "Requests data/photo corrections and transitions order to CORRECTION_REQUIRED")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> requestCorrection(
            @PathVariable UUID id,
            @Valid @RequestBody RequestCorrectionRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.requestCorrection(id, adminUserId, request)));
    }

    @PostMapping("/{id}/quotation")
    @Operation(summary = "Generate quotation", description = "Generates commercial quotation with unit pricing and tax")
    public ResponseEntity<ApiResponse<QuotationResponse>> generateQuotation(
            @PathVariable UUID id,
            @Valid @RequestBody GenerateQuotationRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        QuotationResponse response = orderService.generateQuotation(id, adminUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
