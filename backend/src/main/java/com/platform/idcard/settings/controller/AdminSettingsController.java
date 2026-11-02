package com.platform.idcard.settings.controller;

import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.settings.dto.PlatformSettingsResponse;
import com.platform.idcard.settings.dto.UpdatePlatformSettingsRequest;
import com.platform.idcard.settings.service.PlatformSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Platform Settings", description = "Admin configuration for printing charges, agency rates, and operations")
public class AdminSettingsController {

    private final PlatformSettingService settingService;

    public AdminSettingsController(PlatformSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    @Operation(summary = "Get platform settings", description = "Fetches active base printing charges, taxes, and manufacturing parameters")
    public ResponseEntity<ApiResponse<PlatformSettingsResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.ok(settingService.getSettings()));
    }

    @PutMapping
    @Operation(summary = "Update platform settings", description = "Updates platform-wide base printing charges, taxes, and operational rules")
    public ResponseEntity<ApiResponse<PlatformSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdatePlatformSettingsRequest request
    ) {
        PlatformSettingsResponse response = settingService.updateSettings(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
