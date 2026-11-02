package com.platform.idcard.student.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.student.dto.PhotoImportBatchResponse;
import com.platform.idcard.student.dto.StudentPhotoUploadResponse;
import com.platform.idcard.student.service.PhotoImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school")
@Tag(name = "School Photo Import", description = "Student photo matching and bulk upload management")
@PreAuthorize("hasRole('SCHOOL')")
public class SchoolPhotoImportController {

    private final PhotoImportService photoImportService;

    public SchoolPhotoImportController(PhotoImportService photoImportService) {
        this.photoImportService = photoImportService;
    }

    @PostMapping(value = "/photos/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ZIP archive of student photos and perform automatic matching")
    public ResponseEntity<ApiResponse<PhotoImportBatchResponse>> bulkUploadPhotos(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String section
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        PhotoImportBatchResponse response = photoImportService.processBulkPhotoUpload(schoolId, file, className, section);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/photos/bulk-upload/{batchId}")
    @Operation(summary = "Get photo import match report for a bulk upload batch")
    public ResponseEntity<ApiResponse<PhotoImportBatchResponse>> getPhotoBatchReport(
            @PathVariable UUID batchId
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        PhotoImportBatchResponse response = photoImportService.getPhotoBatchReport(schoolId, batchId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping(value = "/students/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Assign or replace single student photo")
    public ResponseEntity<ApiResponse<StudentPhotoUploadResponse>> uploadStudentPhoto(
            @PathVariable("id") UUID studentId,
            @RequestParam("file") MultipartFile file
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        StudentPhotoUploadResponse response = photoImportService.updateStudentPhoto(schoolId, studentId, file);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/photos/results/{resultId}/assign/{studentId}")
    @Operation(summary = "Manually link an unmatched or ambiguous photo to an enrolled student")
    public ResponseEntity<ApiResponse<PhotoImportBatchResponse>> assignPhotoResult(
            @PathVariable UUID resultId,
            @PathVariable UUID studentId
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        PhotoImportBatchResponse response = photoImportService.assignPhotoResultToStudent(schoolId, resultId, studentId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
