package com.platform.idcard.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationUserRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Valid email format is required")
        String email,

        @Size(max = 20, message = "Mobile cannot exceed 20 characters")
        String mobile,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String password,

        @Size(max = 100, message = "Designation cannot exceed 100 characters")
        String designation
) {}
