package com.platform.idcard.audit.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorUserId,
        String actorName,
        String actorRole,
        String action,
        String entityType,
        String entityId,
        String details,
        String ipAddress,
        OffsetDateTime createdAt
) {}
