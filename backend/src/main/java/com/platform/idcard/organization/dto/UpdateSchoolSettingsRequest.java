package com.platform.idcard.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSchoolSettingsRequest(
        @NotBlank(message = "School name is required")
        @Size(max = 200, message = "School name cannot exceed 200 characters")
        String schoolName,

        @Size(max = 300, message = "Address cannot exceed 300 characters")
        String address,

        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @Size(max = 100, message = "State cannot exceed 100 characters")
        String state,

        @Size(max = 20, message = "Pincode cannot exceed 20 characters")
        String pincode,

        @Size(max = 150, message = "Email cannot exceed 150 characters")
        String email,

        @Size(max = 20, message = "Mobile cannot exceed 20 characters")
        String mobile,

        @Size(max = 255, message = "Logo file ID cannot exceed 255 characters")
        String logoFileId,

        @Size(max = 255, message = "Signature file ID cannot exceed 255 characters")
        String signatureFileId,

        @Size(max = 200, message = "Website URL cannot exceed 200 characters")
        String website,

        @Size(max = 100, message = "Affiliation number cannot exceed 100 characters")
        String affiliationNumber,

        @Size(max = 300, message = "Tagline cannot exceed 300 characters")
        String tagline,

        @Size(max = 150, message = "Principal name cannot exceed 150 characters")
        String principalName
) {}
