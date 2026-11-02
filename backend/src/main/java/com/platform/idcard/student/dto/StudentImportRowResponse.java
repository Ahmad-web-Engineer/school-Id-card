package com.platform.idcard.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.platform.idcard.student.entity.StudentImportRow;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentImportRowResponse(
        UUID id,
        int rowNumber,
        String admissionNumber,
        String studentName,
        String className,
        String section,
        String rollNumber,
        String gender,
        String bloodGroup,
        String mobile,
        String validationStatus,
        String errorMessage
) {
    public static StudentImportRowResponse fromEntity(StudentImportRow row) {
        return new StudentImportRowResponse(
                row.getId(),
                row.getRowNumber(),
                row.getAdmissionNumber(),
                row.getStudentName(),
                row.getClassName(),
                row.getSection(),
                row.getRollNumber(),
                row.getGender(),
                row.getBloodGroup(),
                row.getMobile(),
                row.getValidationStatus(),
                row.getErrorMessage()
        );
    }
}
