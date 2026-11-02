package com.platform.idcard.template.dto;

import java.util.List;
import java.util.UUID;

public record CardTemplateDetailResponse(
        UUID id,
        String templateName,
        String templateCategory,
        UUID schoolId,
        String status,
        List<TemplateVersionResponse> versions
) {}
