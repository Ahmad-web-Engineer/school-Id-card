package com.platform.idcard.finance.dto;

import java.math.BigDecimal;

public record AdminFinanceSummaryResponse(
        BigDecimal grossRevenue,
        BigDecimal collectedRevenue,
        BigDecimal outstandingReceivables,
        BigDecimal agencyDirectCost,
        BigDecimal agencySettledPayouts,
        BigDecimal agencyPendingPayables,
        BigDecimal grossContribution,
        BigDecimal grossMarginPercentage
) {}
