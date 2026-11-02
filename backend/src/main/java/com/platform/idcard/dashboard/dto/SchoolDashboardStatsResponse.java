package com.platform.idcard.dashboard.dto;

import java.util.List;

public record SchoolDashboardStatsResponse(
        long enrolledStudents,
        long approvedTemplates,
        long totalOrders,
        long activeOrders,
        long deliveredCards,
        long pendingQuotations,
        List<RecentOrderSummary> recentOrders
) {}
