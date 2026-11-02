package com.platform.idcard.production.dto;

import java.util.UUID;

public record StartProductionResponse(
        UUID assignmentId,
        String jobNumber,
        String assignmentStatus,
        String message
) {}
