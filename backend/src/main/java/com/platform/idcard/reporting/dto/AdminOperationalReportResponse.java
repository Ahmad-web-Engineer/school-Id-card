package com.platform.idcard.reporting.dto;

import java.util.Map;

public record AdminOperationalReportResponse(
        long totalOrders,
        Map<String, Long> ordersByStatus,
        long totalStudents,
        long totalCardsPrinted,
        long totalCardsDelivered,
        long activeAgencies,
        long activeSchools,
        double averageQualityPassRate
) {}
