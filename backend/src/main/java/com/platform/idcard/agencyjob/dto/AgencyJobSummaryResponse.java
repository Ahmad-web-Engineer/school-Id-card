package com.platform.idcard.agencyjob.dto;

import com.platform.idcard.agencyjob.entity.AssignmentStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AgencyJobSummaryResponse(
        UUID id,
        String jobNumber,
        String academicSession,
        Integer totalQuantity,
        LocalDate expectedDeliveryDate,
        AssignmentStatus assignmentStatus,
        OffsetDateTime assignedAt
) {}
