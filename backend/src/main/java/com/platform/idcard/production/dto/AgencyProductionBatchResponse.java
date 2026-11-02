package com.platform.idcard.production.dto;

import com.platform.idcard.production.entity.ProductionBatchStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgencyProductionBatchResponse(
        UUID id,
        UUID assignmentId,
        String batchNumber,
        Integer plannedQuantity,
        Integer actualQuantity,
        ProductionBatchStatus productionStatus,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String notes,
        OffsetDateTime createdAt
) {}
