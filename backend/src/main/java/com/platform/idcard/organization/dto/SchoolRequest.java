package com.platform.idcard.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SchoolRequest(
        @NotBlank(message = "School name is required")
        @Size(max = 200, message = "School name cannot exceed 200 characters")
        String schoolName,

        @NotBlank(message = "School code is required")
        @Size(max = 50, message = "School code cannot exceed 50 characters")
        String schoolCode,

        @Size(max = 300, message = "Address cannot exceed 300 characters")
        String address,

        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @Size(max = 100, message = "State cannot exceed 100 characters")
        String state,

        @Size(max = 20, message = "Pincode cannot exceed 20 characters")
        String pincode,

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email format is required")
        String email,

        @Size(max = 20, message = "Mobile cannot exceed 20 characters")
        String mobile,

        String status
) {}
