package com.platform.idcard.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String notificationType,
        String title,
        String message,
        String referenceType,
        UUID referenceId,
        boolean read,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {}
