package com.platform.idcard.organization.dto;

import com.platform.idcard.organization.entity.School;

import java.time.Instant;
import java.util.UUID;

public record SchoolResponse(
        UUID id,
        String schoolName,
        String schoolCode,
        String address,
        String city,
        String state,
        String pincode,
        String email,
        String mobile,
        String logoFileId,
        String signatureFileId,
        String website,
        String affiliationNumber,
        String tagline,
        String principalName,
        String status,
        Instant createdAt
) {
    public static SchoolResponse fromEntity(School school) {
        return new SchoolResponse(
                school.getId(),
                school.getSchoolName(),
                school.getSchoolCode(),
                school.getAddress(),
                school.getCity(),
                school.getState(),
                school.getPincode(),
                school.getEmail(),
                school.getMobile(),
                school.getLogoFileId(),
                school.getSignatureFileId(),
                school.getWebsite(),
                school.getAffiliationNumber(),
                school.getTagline(),
                school.getPrincipalName(),
                school.getStatus(),
                school.getCreatedAt()
        );
    }
}
