package com.platform.idcard.production.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.production.dto.SchoolOrderTrackingResponse;
import com.platform.idcard.production.service.ProductionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/orders")
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "School Order Tracking", description = "Live order production and quality verification tracking")
public class SchoolOrderTrackingController {

    private final ProductionService productionService;

    public SchoolOrderTrackingController(ProductionService productionService) {
        this.productionService = productionService;
    }

    @GetMapping("/{id}/tracking")
    @Operation(summary = "Track order production", description = "Live production and quality progress tracking without exposing agency data")
    public ResponseEntity<ApiResponse<SchoolOrderTrackingResponse>> getOrderTracking(@PathVariable UUID id) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        SchoolOrderTrackingResponse response = productionService.getSchoolOrderTracking(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
