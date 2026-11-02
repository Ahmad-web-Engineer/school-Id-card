package com.platform.idcard.student.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.student.dto.StudentImportBatchDetailResponse;
import com.platform.idcard.student.dto.StudentImportBatchResponse;
import com.platform.idcard.student.dto.StudentImportConfirmResponse;
import com.platform.idcard.student.service.StudentImportService;
import com.platform.idcard.student.service.StudentTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/students")
@Tag(name = "School Student Import", description = "Student bulk import, spreadsheet validation, and error reporting")
@PreAuthorize("hasRole('SCHOOL')")
public class SchoolStudentImportController {

    private final StudentTemplateService templateService;
    private final StudentImportService importService;

    public SchoolStudentImportController(
            StudentTemplateService templateService,
            StudentImportService importService
    ) {
        this.templateService = templateService;
        this.importService = importService;
    }

    @GetMapping("/import-template")
    @Operation(summary = "Download standard XLSX or CSV student import template with example rows")
    public ResponseEntity<byte[]> downloadTemplate(
            @RequestParam(defaultValue = "xlsx") String format
    ) {
        if ("csv".equalsIgnoreCase(format)) {
            byte[] csvBytes = templateService.generateCsvTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"student_import_template.csv\"")
                    .body(csvBytes);
        } else {
            byte[] xlsxBytes = templateService.generateXlsxTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"student_import_template.xlsx\"")
                    .body(xlsxBytes);
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and validate student import file (Excel or CSV)")
    public ResponseEntity<ApiResponse<StudentImportBatchResponse>> uploadImportFile(
            @RequestParam("file") MultipartFile file
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        StudentImportBatchResponse response = importService.processImport(schoolId, file);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/import/{batchId}")
    @Operation(summary = "Get student import batch details, preview rows, and pagination")
    public ResponseEntity<StudentImportBatchDetailResponse> getBatchDetails(
            @PathVariable UUID batchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        StudentImportBatchDetailResponse response = importService.getBatchDetails(schoolId, batchId, page, size, status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/import/{batchId}/confirm")
    @Operation(summary = "Confirm and commit valid staging rows into students table")
    public ResponseEntity<ApiResponse<StudentImportConfirmResponse>> confirmImport(
            @PathVariable UUID batchId
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        StudentImportConfirmResponse response = importService.confirmBatchImport(schoolId, batchId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/import/{batchId}/errors")
    @Operation(summary = "Download error report CSV for invalid rows in batch")
    public ResponseEntity<byte[]> downloadErrorReport(
            @PathVariable UUID batchId
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        byte[] csvBytes = importService.generateErrorReportCsv(schoolId, batchId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import_errors_" + batchId + ".csv\"")
                .body(csvBytes);
    }
}
