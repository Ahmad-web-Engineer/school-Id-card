package com.platform.idcard.delivery.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.delivery.dto.AgencyDispatchResponse;
import com.platform.idcard.delivery.dto.CreateDispatchPayload;
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
@RequestMapping("/api/v1/agency")
@PreAuthorize("hasRole('PRINTING_AGENCY')")
@Tag(name = "Agency Dispatch", description = "Agency consignment dispatches, courier tracking, and consignment listings")
public class AgencyDispatchController {

    private final DeliveryService deliveryService;

    public AgencyDispatchController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping("/jobs/{id}/dispatch")
    @Operation(summary = "Create dispatch record", description = "Dispatches a finished print run with courier name and tracking code")
    public ResponseEntity<ApiResponse<AgencyDispatchResponse>> dispatchJob(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDispatchPayload payload
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        UUID userId = SecurityUtils.getCurrentUserId();
        AgencyDispatchResponse response = deliveryService.agencyCreateDispatch(id, agencyId, userId, payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/jobs/{id}/dispatches")
    @Operation(summary = "List job dispatches", description = "Paginated dispatches associated with a specific job assignment")
    public ResponseEntity<ApiResponse<PagedResponse<AgencyDispatchResponse>>> listJobDispatches(
            @PathVariable UUID id,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        PagedResponse<AgencyDispatchResponse> response = deliveryService.getAgencyJobDispatches(id, agencyId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/dispatches")
    @Operation(summary = "List agency dispatches", description = "Paginated listing of all consignments dispatched by the authenticated agency")
    public ResponseEntity<ApiResponse<PagedResponse<AgencyDispatchResponse>>> listAllDispatches(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID agencyId = SecurityUtils.getRequiredAgencyId();
        PagedResponse<AgencyDispatchResponse> response = deliveryService.getAgencyDispatches(agencyId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
