package com.platform.idcard.finance.dto;

import java.math.BigDecimal;

public record SchoolFinanceSummaryResponse(
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal outstandingBalance,
        long invoiceCount,
        long paymentCount
) {}
