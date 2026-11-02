package com.platform.idcard.agencyjob.dto;

import java.util.UUID;

public record AgencyJobItemResponse(
        UUID id,
        String productType,
        Integer assignedQuantity,
        Integer printedQuantity,
        Integer rejectedQuantity,
        Integer acceptedQuantity,
        Integer pendingQuantity
) {}
