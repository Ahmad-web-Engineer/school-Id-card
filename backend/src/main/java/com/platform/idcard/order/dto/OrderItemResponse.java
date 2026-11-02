package com.platform.idcard.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        String productType,
        Integer quantity,
        BigDecimal unitSellingPrice,
        BigDecimal itemTotal
) {}
