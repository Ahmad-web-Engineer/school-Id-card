package com.platform.idcard.agencyjob.dto;

import com.platform.idcard.agencyjob.entity.AssignmentStatus;
import com.platform.idcard.order.dto.OrderStudentSnapshotResponse;
import com.platform.idcard.template.dto.PrintSettingsDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminAgencyAssignmentDetailResponse(
        UUID id,
        String assignmentNumber,
        UUID orderId,
        String orderNumber,
        String schoolName,
        UUID agencyId,
        String agencyName,
        String agencyContactPerson,
        String agencyEmail,
        String agencyPhone,
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
        List<AdminAgencyJobItemResponse> jobItems,
        AgencyCardSpecificationsResponse specs,
        List<OrderStudentSnapshotResponse> students,
        PrintSettingsDto printSettings,
        Boolean allowAgencyOverride
) {
    public AdminAgencyAssignmentDetailResponse(
            UUID id,
            String assignmentNumber,
            UUID orderId,
            String orderNumber,
            String schoolName,
            UUID agencyId,
            String agencyName,
            String agencyContactPerson,
            String agencyEmail,
            String agencyPhone,
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
            List<AdminAgencyJobItemResponse> jobItems,
            AgencyCardSpecificationsResponse specs,
            List<OrderStudentSnapshotResponse> students
    ) {
        this(id, assignmentNumber, orderId, orderNumber, schoolName, agencyId, agencyName, agencyContactPerson, agencyEmail, agencyPhone, assignedByName, assignedAt, expectedDeliveryDate, agreedRate, totalAgreedAmount, assignmentStatus, reassignedFromId, reassignmentReason, rejectionReason, notes, totalAssignedQuantity, jobItems, specs, students, null, false);
    }
}
