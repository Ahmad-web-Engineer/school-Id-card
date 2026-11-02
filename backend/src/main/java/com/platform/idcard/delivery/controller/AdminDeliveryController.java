package com.platform.idcard.delivery.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.delivery.dto.*;
import com.platform.idcard.delivery.service.DeliveryService;
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
@RequestMapping("/api/v1/admin/delivery")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Delivery & Logistics", description = "Global consignment tracking, Model B hub receipts, secondary dispatches, and delivery closure")
public class AdminDeliveryController {

    private final DeliveryService deliveryService;

    public AdminDeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/dispatches")
    @Operation(summary = "List all global dispatches", description = "Paginated listing of all dispatches across agencies and orders")
    public ResponseEntity<ApiResponse<PagedResponse<AdminDispatchResponse>>> listDispatches(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID agencyId,
            @RequestParam(required = false) String status
    ) {
        PagedResponse<AdminDispatchResponse> response = deliveryService.getAdminDispatches(pageable, agencyId, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/dispatches/{id}/receive-hub")
    @Operation(summary = "Receive consignment at hub", description = "Model B: Admin marks dispatched package received at central sorting hub")
    public ResponseEntity<ApiResponse<AdminDispatchResponse>> receiveAtHub(
            @PathVariable UUID id,
            @Valid @RequestBody HubReceiveRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminDispatchResponse response = deliveryService.adminReceiveAtHub(id, adminUserId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/orders/{id}/dispatch-to-school")
    @Operation(summary = "Secondary dispatch from hub to school", description = "Model B: Arranges courier from central admin hub to school")
    public ResponseEntity<ApiResponse<AdminDispatchResponse>> secondaryDispatch(
            @PathVariable UUID id,
            @Valid @RequestBody AdminSecondaryDispatchRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminDispatchResponse response = deliveryService.adminSecondaryDispatch(id, adminUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/records")
    @Operation(summary = "List all delivery records", description = "Paginated list of confirmed deliveries across schools")
    public ResponseEntity<ApiResponse<PagedResponse<AdminDeliveryResponse>>> listDeliveries(
            @PageableDefault(size = 10, sort = "deliveredAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) String status
    ) {
        PagedResponse<AdminDeliveryResponse> response = deliveryService.getAdminDeliveries(pageable, schoolId, status);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/records/{id}/close")
    @Operation(summary = "Close delivery record", description = "Concludes the delivery lifecycle after school verification")
    public ResponseEntity<ApiResponse<AdminDeliveryResponse>> closeDelivery(
            @PathVariable UUID id,
            @RequestBody(required = false) CloseDeliveryRequest request
    ) {
        UUID adminUserId = SecurityUtils.getCurrentUserId();
        AdminDeliveryResponse response = deliveryService.adminCloseDelivery(id, adminUserId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
