package com.platform.idcard.finance.dto;

import com.platform.idcard.finance.entity.PaymentMethod;
import com.platform.idcard.finance.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminPaymentResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        UUID schoolId,
        String schoolName,
        String paymentReference,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String transactionReference,
        OffsetDateTime receivedAt,
        String recordedByName,
        String paymentNotes
) {}
