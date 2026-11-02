package com.platform.idcard.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTemplateRequest(
        @NotNull(message = "School ID is required")
        UUID schoolId,

        @NotBlank(message = "Academic session is required")
        String academicSession,

        Boolean active
) {}
