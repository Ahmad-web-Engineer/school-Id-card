package com.platform.idcard.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecentOrderSummary(
        UUID orderId,
        String orderNumber,
        String schoolName,
        int totalStudents,
        int totalQuantity,
        String orderStatus,
        Instant createdAt
) {}
