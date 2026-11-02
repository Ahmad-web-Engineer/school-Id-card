package com.platform.idcard.delivery.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.delivery.dto.ConfirmDeliveryRequest;
import com.platform.idcard.delivery.dto.SchoolDeliveryResponse;
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
@RequestMapping("/api/v1/school/orders/{id}")
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "School Delivery Receipt", description = "School order delivery records and recipient confirmation")
public class SchoolDeliveryController {

    private final DeliveryService deliveryService;

    public SchoolDeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/deliveries")
    @Operation(summary = "View order deliveries", description = "Lists delivery confirmations and shipment status for the school order")
    public ResponseEntity<ApiResponse<PagedResponse<SchoolDeliveryResponse>>> listOrderDeliveries(
            @PathVariable UUID id,
            @PageableDefault(size = 10, sort = "deliveredAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        PagedResponse<SchoolDeliveryResponse> response = deliveryService.getSchoolDeliveries(id, schoolId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/confirm-delivery")
    @Operation(summary = "Confirm delivery receipt", description = "School acknowledges receipt of printed ID card consignment")
    public ResponseEntity<ApiResponse<SchoolDeliveryResponse>> confirmDelivery(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmDeliveryRequest request
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        SchoolDeliveryResponse response = deliveryService.schoolConfirmDelivery(id, schoolId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
