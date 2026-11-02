package com.platform.idcard.agencyjob.dto;

import com.platform.idcard.agencyjob.entity.AssignmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminAgencyAssignmentResponse(
        UUID id,
        String assignmentNumber,
        UUID orderId,
        String orderNumber,
        String schoolName,
        UUID agencyId,
        String agencyName,
        String assignedByName,
        OffsetDateTime assignedAt,
        LocalDate expectedDeliveryDate,
        BigDecimal agreedRate,
        BigDecimal totalAgreedAmount,
        AssignmentStatus assignmentStatus,
        UUID reassignedFromId,
        String reassignmentReason,
        String rejectionReason,
        String notes,
        Integer totalAssignedQuantity,
        List<AdminAgencyJobItemResponse> jobItems
) {}
