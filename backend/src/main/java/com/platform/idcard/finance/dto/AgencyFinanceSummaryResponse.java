package com.platform.idcard.finance.dto;

import java.math.BigDecimal;

public record AgencyFinanceSummaryResponse(
        BigDecimal totalAgreedAmount,
        BigDecimal totalDeductions,
        BigDecimal totalPayableAmount,
        BigDecimal totalPaidAmount,
        BigDecimal outstandingPayable,
        long settlementCount
) {}
