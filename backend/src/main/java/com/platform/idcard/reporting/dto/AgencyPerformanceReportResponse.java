package com.platform.idcard.reporting.dto;

import java.math.BigDecimal;

public record AgencyPerformanceReportResponse(
        long totalJobsAssigned,
        long totalCardsAssigned,
        long totalCardsProduced,
        long totalCardsDispatched,
        long qcPassedQuantity,
        long qcFailedQuantity,
        double qcPassPercentage,
        BigDecimal totalEarnings,
        BigDecimal settledEarnings,
        BigDecimal pendingPayout
) {}
