package com.platform.idcard.template.dto;

import java.time.Instant;
import java.util.UUID;

public record CardTemplateResponse(
        UUID id,
        String templateName,
        String templateCategory,
        UUID schoolId,
        String schoolName,
        String status,
        Integer latestVersionNumber,
        String activeVersionApprovalStatus,
        Instant createdAt
) {}
