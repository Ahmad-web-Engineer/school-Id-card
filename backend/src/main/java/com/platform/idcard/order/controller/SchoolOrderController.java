package com.platform.idcard.order.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.order.dto.CreateOrderRequest;
import com.platform.idcard.order.dto.SchoolOrderDetailResponse;
import com.platform.idcard.order.dto.SchoolOrderSummaryResponse;
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
@RequestMapping("/api/v1/school/orders")
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "School Orders", description = "School ID card ordering and quotation approval endpoints")
public class SchoolOrderController {

    private final OrderService orderService;

    public SchoolOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Create draft order", description = "Places an ID card order with student snapshots in DRAFT status")
    public ResponseEntity<ApiResponse<SchoolOrderDetailResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        UUID userId = SecurityUtils.getCurrentUserId();
        SchoolOrderDetailResponse response = orderService.createOrder(schoolId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    @Operation(summary = "List school orders", description = "Paginated list of orders placed by caller's school")
    public ResponseEntity<PagedResponse<SchoolOrderSummaryResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        return ResponseEntity.ok(orderService.getSchoolOrders(schoolId, page, size, search, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order details", description = "Retrieves order details, items, student snapshots, and quotation")
    public ResponseEntity<ApiResponse<SchoolOrderDetailResponse>> getOrderById(@PathVariable UUID id) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.getSchoolOrderById(id, schoolId)));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit order for review", description = "Transitions a DRAFT or CORRECTION_REQUIRED order to SUBMITTED")
    public ResponseEntity<ApiResponse<SchoolOrderDetailResponse>> submitOrder(@PathVariable UUID id) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.submitOrder(id, schoolId)));
    }

    @PostMapping("/{id}/approve-quotation")
    @Operation(summary = "Approve quotation", description = "Approves commercial quotation and transitions order to APPROVED")
    public ResponseEntity<ApiResponse<SchoolOrderDetailResponse>> approveQuotation(@PathVariable UUID id) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        UUID userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.approveQuotation(id, schoolId, userId)));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels a draft or submitted order")
    public ResponseEntity<ApiResponse<SchoolOrderDetailResponse>> cancelOrder(@PathVariable UUID id) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        return ResponseEntity.ok(ApiResponse.ok(orderService.cancelOrder(id, schoolId)));
    }
}
