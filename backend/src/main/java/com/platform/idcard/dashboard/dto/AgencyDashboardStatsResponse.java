package com.platform.idcard.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AgencyDashboardStatsResponse(
        long totalJobs,
        long pendingJobs,
        long inProductionJobs,
        long completedJobs,
        long productionCapacity,
        BigDecimal totalEarnings,
        List<RecentAgencyJobSummary> recentJobs
) {
    public record RecentAgencyJobSummary(
            UUID assignmentId,
            String assignmentNumber,
            int assignedQuantity,
            String assignmentStatus,
            BigDecimal agreedRate
    ) {}
}
