package com.platform.idcard.production.dto;

import java.util.UUID;

public record SchoolOrderTrackingResponse(
        UUID orderId,
        String orderNumber,
        String orderStatus,
        Integer totalCards,
        Integer cardsPrinted,
        Integer cardsVerified,
        Double percentComplete,
        String currentStage,
        String stageDescription
) {}
