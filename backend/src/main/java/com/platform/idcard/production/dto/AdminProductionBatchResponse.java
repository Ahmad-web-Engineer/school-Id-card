package com.platform.idcard.production.dto;

import com.platform.idcard.production.entity.ProductionBatchStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminProductionBatchResponse(
        UUID id,
        UUID assignmentId,
        String jobNumber,
        UUID agencyId,
        String agencyName,
        UUID orderId,
        String orderNumber,
        String batchNumber,
        Integer plannedQuantity,
        Integer actualQuantity,
        ProductionBatchStatus productionStatus,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String notes,
        OffsetDateTime createdAt
) {}
