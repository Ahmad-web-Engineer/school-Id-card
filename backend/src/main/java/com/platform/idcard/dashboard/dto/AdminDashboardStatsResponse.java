package com.platform.idcard.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminDashboardStatsResponse(
        long totalSchools,
        long totalAgencies,
        long totalStudents,
        long totalOrders,
        long activeOrders,
        long completedOrders,
        BigDecimal totalRevenue,
        List<RecentOrderSummary> recentOrders,
        Map<String, Long> statusDistribution
) {}
