package com.platform.idcard.delivery.dto;

import com.platform.idcard.delivery.entity.DispatchStatus;
import com.platform.idcard.delivery.entity.DispatchType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgencyDispatchResponse(
        UUID id,
        UUID assignmentId,
        String jobNumber,
        String dispatchNumber,
        String courierName,
        String trackingNumber,
        Integer dispatchQuantity,
        DispatchStatus dispatchStatus,
        DispatchType dispatchType,
        OffsetDateTime dispatchDate,
        String dispatchNotes
) {}
