package com.platform.idcard.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgencyRequest(
        @NotBlank(message = "Agency name is required")
        @Size(max = 200, message = "Agency name cannot exceed 200 characters")
        String agencyName,

        @NotBlank(message = "Contact person is required")
        @Size(max = 100, message = "Contact person cannot exceed 100 characters")
        String contactPerson,

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email format is required")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Size(max = 20, message = "Mobile cannot exceed 20 characters")
        String mobile,

        @Size(max = 300, message = "Address cannot exceed 300 characters")
        String address,

        @Min(value = 1, message = "Production capacity must be at least 1")
        Integer productionCapacity,

        @Size(max = 500, message = "Capabilities description cannot exceed 500 characters")
        String capabilities,

        String status
) {}
