package com.platform.idcard.agencyjob.dto;

import com.platform.idcard.agencyjob.entity.AssignmentStatus;
import com.platform.idcard.order.dto.OrderStudentSnapshotResponse;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AgencyJobDetailResponse(
        UUID id,
        String jobNumber,
        String academicSession,
        Integer totalQuantity,
        LocalDate expectedDeliveryDate,
        AssignmentStatus assignmentStatus,
        OffsetDateTime assignedAt,
        String notes,
        String rejectionReason,
        AgencyCardSpecificationsResponse cardSpecifications,
        List<AgencyJobItemResponse> items,
        List<OrderStudentSnapshotResponse> students
) {}
