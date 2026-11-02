package com.platform.idcard.production.dto;

import com.platform.idcard.production.entity.DefectCategory;
import com.platform.idcard.production.entity.QualityCheckStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record QualityCheckResponse(
        UUID id,
        UUID assignmentId,
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
