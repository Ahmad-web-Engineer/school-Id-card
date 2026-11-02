package com.platform.idcard.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record QuotationItemResponse(
        UUID id,
        String productDescription,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal amount
) {}
