package com.platform.idcard.student.dto;

import java.util.UUID;

public record StudentPhotoUploadResponse(
        UUID studentId,
        UUID photoFileId,
        String photoUrl,
        String message
) {}
