package com.platform.idcard.agencyjob.dto;

import java.util.UUID;

public record AdminAgencyJobItemResponse(
        UUID id,
        UUID orderItemId,
        String productType,
        Integer assignedQuantity,
        Integer printedQuantity,
        Integer rejectedQuantity,
        Integer acceptedQuantity,
        Integer pendingQuantity
) {}
