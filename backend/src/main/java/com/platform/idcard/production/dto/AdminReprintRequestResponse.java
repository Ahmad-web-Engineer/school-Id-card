package com.platform.idcard.production.dto;

import com.platform.idcard.production.entity.CostBearer;
import com.platform.idcard.production.entity.ReprintStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminReprintRequestResponse(
        UUID id,
        UUID assignmentId,
        String jobNumber,
        UUID qualityCheckId,
        String requestNumber,
        Integer replacementQuantity,
        String reason,
        CostBearer costBearer,
        ReprintStatus approvalStatus,
        String approverName,
        OffsetDateTime approvedAt,
        String rejectionReason,
        OffsetDateTime createdAt
) {}
