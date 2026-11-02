package com.platform.idcard.template.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TemplateVersionResponse(
        UUID id,
        Integer versionNumber,
        BigDecimal cardWidthMm,
        BigDecimal cardHeightMm,
        BigDecimal bleedMarginMm,
        String frontDesignJson,
        String backDesignJson,
        String approvalStatus,
        UUID createdBy,
        UUID approvedBy,
        Instant approvedAt,
        Instant createdAt
) {}
