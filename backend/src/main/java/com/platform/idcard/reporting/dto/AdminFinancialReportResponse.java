package com.platform.idcard.reporting.dto;

import java.math.BigDecimal;

public record AdminFinancialReportResponse(
        BigDecimal grossRevenue,
        BigDecimal collectedRevenue,
        BigDecimal agencyDirectCosts,
        BigDecimal agencySettledPayouts,
        BigDecimal outstandingReceivables,
        BigDecimal agencyPendingPayables,
        BigDecimal grossContribution,
        BigDecimal grossMarginPercentage
) {}
