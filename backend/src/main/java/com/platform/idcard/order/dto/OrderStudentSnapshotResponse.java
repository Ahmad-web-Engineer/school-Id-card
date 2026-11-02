package com.platform.idcard.order.dto;

import java.time.LocalDate;
import java.util.UUID;

public record OrderStudentSnapshotResponse(
        UUID id,
        UUID studentId,
        String admissionNumber,
        String studentName,
        String fatherName,
        String motherName,
        LocalDate dateOfBirth,
        String gender,
        String bloodGroup,
        String className,
        String section,
        String rollNumber,
        UUID photoFileId
) {}
