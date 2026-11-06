package com.platform.idcard.agencyjob.dto;

import com.platform.idcard.agencyjob.entity.AssignmentStatus;
import com.platform.idcard.order.dto.OrderStudentSnapshotResponse;
import com.platform.idcard.template.dto.PrintSettingsDto;

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
        List<OrderStudentSnapshotResponse> students,
        PrintSettingsDto printSettings,
        Boolean allowAgencyOverride
) {
    public AgencyJobDetailResponse(
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
    ) {
        this(id, jobNumber, academicSession, totalQuantity, expectedDeliveryDate, assignmentStatus, assignedAt, notes, rejectionReason, cardSpecifications, items, students, null, false);
    }
}
