package com.platform.idcard.production.dto;

import com.platform.idcard.production.entity.DefectCategory;
import com.platform.idcard.production.entity.QualityCheckStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminQualityCheckResponse(
        UUID id,
        UUID assignmentId,
        String jobNumber,
        UUID agencyId,
        String agencyName,
        UUID orderId,
        String orderNumber,
        String checkNumber,
        Integer inspectedQuantity,
        Integer acceptedQuantity,
        Integer rejectedQuantity,
        DefectCategory defectCategory,
        String defectDescription,
        QualityCheckStatus inspectionStatus,
        String inspectorName,
        String notes,
        OffsetDateTime inspectedAt
) {}
