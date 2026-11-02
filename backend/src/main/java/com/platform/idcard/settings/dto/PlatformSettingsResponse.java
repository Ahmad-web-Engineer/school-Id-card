package com.platform.idcard.settings.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PlatformSettingsResponse(
        BigDecimal pricingPvcCard,
        BigDecimal pricingRfidCard,
        BigDecimal pricingLanyard,
        BigDecimal pricingCardHolder,
        BigDecimal pricingRushFee,
        BigDecimal agencyCostPerCard,
        BigDecimal taxRatePercent,
        BigDecimal flatShippingFee,
        Integer minOrderQuantity,
        Integer paymentDueDays,
        Integer quotationValidityDays,
        String defaultDeliveryModel,
        String platformCurrency,
        Instant lastUpdatedAt
) {}
