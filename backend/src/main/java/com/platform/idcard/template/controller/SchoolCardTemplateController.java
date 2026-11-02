package com.platform.idcard.template.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.template.dto.SchoolTemplateResponse;
import com.platform.idcard.template.service.CardTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/templates")
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "School ID Card Templates", description = "School ID card template catalog and preview with enrolled students")
public class SchoolCardTemplateController {

    private final CardTemplateService cardTemplateService;

    public SchoolCardTemplateController(CardTemplateService cardTemplateService) {
        this.cardTemplateService = cardTemplateService;
    }

    @GetMapping
    @Operation(summary = "List assigned templates", description = "Retrieves approved card templates available to caller's school")
    public ResponseEntity<PagedResponse<SchoolTemplateResponse>> listSchoolTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Non-negotiable Rule 2: School ID strictly extracted from caller token context
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        return ResponseEntity.ok(cardTemplateService.getSchoolTemplates(schoolId, page, size));
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Preview card with enrolled student", description = "Generates high-resolution PDF preview with school student details")
    public ResponseEntity<byte[]> previewSchoolTemplate(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID studentId
    ) {
        // Non-negotiable Rule 2: School ID strictly extracted from caller token context
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        byte[] pdfBytes = cardTemplateService.renderPreviewPdf(id, null, studentId, schoolId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"card_preview.pdf\"")
                .body(pdfBytes);
    }
}
