package com.platform.idcard.order.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SchoolOrderDetailResponse(
        UUID id,
        String orderNumber,
        String academicSession,
        UUID templateVersionId,
        String templateName,
        Integer templateVersionNumber,
        Integer totalStudents,
        Integer totalQuantity,
        String orderStatus,
        String deliveryModel,
        String deliveryAddress,
        String contactPerson,
        String contactMobile,
        String remarks,
        String rejectionRemarks,
        Instant submittedAt,
        Instant verifiedAt,
        Instant createdAt,
        List<OrderItemResponse> items,
        List<OrderStudentSnapshotResponse> students,
        QuotationResponse quotation
) {}
