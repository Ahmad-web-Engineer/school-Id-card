package com.platform.idcard.reporting.dto;

import java.math.BigDecimal;

public record SchoolReportSummaryResponse(
        long totalStudents,
        long totalOrders,
        long cardsOrdered,
        long cardsDelivered,
        BigDecimal totalBilled,
        BigDecimal totalPaid,
        BigDecimal outstandingDues
) {}
