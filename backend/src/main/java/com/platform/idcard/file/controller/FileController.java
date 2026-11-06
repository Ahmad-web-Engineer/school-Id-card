package com.platform.idcard.file.controller;

import com.platform.idcard.agencyjob.repository.AgencyAssignmentRepository;
import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.file.entity.FileEntity;
import com.platform.idcard.file.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files", description = "File storage and retrieval endpoints")
public class FileController {

    private final FileStorageService fileStorageService;
    private final AgencyAssignmentRepository agencyAssignmentRepository;

    public FileController(FileStorageService fileStorageService, AgencyAssignmentRepository agencyAssignmentRepository) {
        this.fileStorageService = fileStorageService;
        this.agencyAssignmentRepository = agencyAssignmentRepository;
    }

    @GetMapping(value = {"/{fileId}", "/{fileId}/download"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL', 'PRINTING_AGENCY')")
    @Operation(summary = "View or download a stored file with tenant access checks")
    public ResponseEntity<Resource> getFile(@PathVariable UUID fileId) {
        FileEntity fileEntity = fileStorageService.getFile(fileId);

        // Tenant access control
        if (SecurityUtils.isSuperAdmin()) {
            // Super Admin has global read access
        } else if (SecurityUtils.isSchoolUser()) {
            UUID schoolId = SecurityUtils.getRequiredSchoolId();
            if (fileEntity.getSchool() == null || !fileEntity.getSchool().getId().equals(schoolId)) {
                throw new ApiException(
                        ErrorCode.ACCESS_DENIED,
                        "You do not have permission to access this file.",
                        HttpStatus.FORBIDDEN
                );
            }
        } else if (SecurityUtils.isAgencyUser()) {
            UUID agencyId = SecurityUtils.getRequiredAgencyId();
            if (fileEntity.getSchool() != null) {
                boolean hasAssignment = agencyAssignmentRepository.existsByAgencyIdAndSchoolId(agencyId, fileEntity.getSchool().getId());
                if (!hasAssignment) {
                    throw new ApiException(
                            ErrorCode.ACCESS_DENIED,
                            "Access denied: Agency is not authorized to access files for this school.",
                            HttpStatus.FORBIDDEN
                    );
                }
            }
        } else {
            throw new ApiException(
                    ErrorCode.ACCESS_DENIED,
                    "You do not have permission to access this file.",
                    HttpStatus.FORBIDDEN
            );
        }

        Resource resource = fileStorageService.loadAsResource(fileEntity);

        String contentType = fileEntity.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=86400")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileEntity.getOriginalFileName() + "\"")
                .body(resource);
    }
}
