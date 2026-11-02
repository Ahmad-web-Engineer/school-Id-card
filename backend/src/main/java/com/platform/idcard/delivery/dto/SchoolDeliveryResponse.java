package com.platform.idcard.delivery.dto;

import com.platform.idcard.delivery.entity.DeliveryModel;
import com.platform.idcard.delivery.entity.DeliveryStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SchoolDeliveryResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        String deliveryNumber,
        DeliveryModel deliveryModel,
        Integer deliveryQuantity,
        String receivedBy,
        String receivedMobile,
        DeliveryStatus deliveryStatus,
        OffsetDateTime deliveredAt,
        String deliveryNotes
) {}
