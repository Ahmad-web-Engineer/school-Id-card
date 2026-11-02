package com.platform.idcard.template.dto;

import java.util.UUID;

public record TemplateAssignmentResponse(
        UUID assignmentId,
        UUID templateId,
        UUID schoolId,
        String academicSession,
        String message
) {}
