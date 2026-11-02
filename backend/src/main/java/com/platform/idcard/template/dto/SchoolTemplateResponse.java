package com.platform.idcard.template.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SchoolTemplateResponse(
        UUID id,
        String templateName,
        String templateCategory,
        UUID approvedVersionId,
        Integer versionNumber,
        BigDecimal cardWidthMm,
        BigDecimal cardHeightMm,
        String frontDesignJson,
        String backDesignJson
) {}
