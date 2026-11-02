package com.platform.idcard.agencyjob.dto;

import java.math.BigDecimal;

public record AgencyCardSpecificationsResponse(
        BigDecimal widthMm,
        BigDecimal heightMm,
        BigDecimal bleedMm,
        String templateName
) {}
