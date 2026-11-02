package com.platform.idcard.auth.dto;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String fullName,
        String email,
        String mobile,
        String role,
        UUID schoolId,
        UUID agencyId,
        String designation,
        String status
) {}
