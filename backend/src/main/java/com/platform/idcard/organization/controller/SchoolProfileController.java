package com.platform.idcard.organization.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.organization.dto.SchoolResponse;
import com.platform.idcard.organization.service.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/profile")
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "School Profile", description = "School self-profile access")
public class SchoolProfileController {

    private final SchoolService schoolService;

    public SchoolProfileController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping
    @Operation(summary = "Get own school profile", description = "Returns school details derived strictly from logged-in token")
    public ResponseEntity<ApiResponse<SchoolResponse>> getOwnProfile() {
        // Non-negotiable rule 2: Read school_id strictly from authenticated session token
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        SchoolResponse response = schoolService.getSchoolById(schoolId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
