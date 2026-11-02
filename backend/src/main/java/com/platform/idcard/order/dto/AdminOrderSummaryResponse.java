package com.platform.idcard.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminOrderSummaryResponse(
        UUID id,
        String orderNumber,
        UUID schoolId,
        String schoolName,
        String schoolCode,
        String academicSession,
        String templateName,
        Integer templateVersionNumber,
        Integer totalStudents,
        Integer totalQuantity,
        String orderStatus,
        BigDecimal quotationAmount,
        Instant submittedAt,
        Instant createdAt
) {}
