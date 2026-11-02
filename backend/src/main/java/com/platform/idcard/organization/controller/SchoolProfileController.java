package com.platform.idcard.organization.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.file.entity.FileEntity;
import com.platform.idcard.file.service.FileStorageService;
import com.platform.idcard.organization.dto.SchoolResponse;
import com.platform.idcard.organization.dto.UpdateSchoolSettingsRequest;
import com.platform.idcard.organization.service.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/profile")
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "School Profile & Settings", description = "School self-profile and settings management")
public class SchoolProfileController {

    private final SchoolService schoolService;
    private final FileStorageService fileStorageService;

    public SchoolProfileController(SchoolService schoolService, FileStorageService fileStorageService) {
        this.schoolService = schoolService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    @Operation(summary = "Get own school profile", description = "Returns school details derived strictly from logged-in token")
    public ResponseEntity<ApiResponse<SchoolResponse>> getOwnProfile() {
        // Non-negotiable rule 2: Read school_id strictly from authenticated session token
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        SchoolResponse response = schoolService.getSchoolById(schoolId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping
    @Operation(summary = "Update school settings", description = "Updates school identity, address, contact, and branding")
    public ResponseEntity<ApiResponse<SchoolResponse>> updateProfile(
            @Valid @RequestBody UpdateSchoolSettingsRequest request
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        SchoolResponse response = schoolService.updateSchoolSettings(schoolId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload school logo", description = "Uploads institutional logo image asset")
    public ResponseEntity<ApiResponse<SchoolResponse>> uploadLogo(
            @RequestParam("file") MultipartFile file
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        UUID userId = SecurityUtils.getCurrentUserId();
        SchoolResponse response = schoolService.uploadSchoolLogo(schoolId, file, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping(value = "/signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload authorized signature", description = "Uploads principal or authorized signature image asset")
    public ResponseEntity<ApiResponse<SchoolResponse>> uploadSignature(
            @RequestParam("file") MultipartFile file
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        UUID userId = SecurityUtils.getCurrentUserId();
        SchoolResponse response = schoolService.uploadSchoolSignature(schoolId, file, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
