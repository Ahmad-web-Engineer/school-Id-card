package com.platform.idcard.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.platform.idcard.student.entity.PhotoImportResult;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PhotoImportResultResponse(
        String fileName,
        String parsedClass,
        String parsedSection,
        String parsedRollNumber,
        UUID studentId,
        String studentName,
        String admissionNumber,
        String matchStatus,
        String message
) {
    public static PhotoImportResultResponse fromEntity(PhotoImportResult entity) {
        return new PhotoImportResultResponse(
                entity.getFileName(),
                entity.getParsedClass(),
                entity.getParsedSection(),
                entity.getParsedRollNumber(),
                entity.getMatchedStudent() != null ? entity.getMatchedStudent().getId() : null,
                entity.getMatchedStudent() != null ? entity.getMatchedStudent().getStudentName() : null,
                entity.getMatchedStudent() != null ? entity.getMatchedStudent().getAdmissionNumber() : null,
                entity.getMatchStatus(),
                entity.getMessage()
        );
    }
}
