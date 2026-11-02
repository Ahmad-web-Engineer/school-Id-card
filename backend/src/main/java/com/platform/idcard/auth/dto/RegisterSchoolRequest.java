package com.platform.idcard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterSchoolRequest(
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

        @NotBlank(message = "School email is required")
        @Email(message = "Valid school email format is required")
        String schoolEmail,

        @Size(max = 20, message = "School mobile cannot exceed 20 characters")
        String schoolMobile,

        @NotBlank(message = "Admin full name is required")
        @Size(max = 100, message = "Admin name cannot exceed 100 characters")
        String adminFullName,

        @NotBlank(message = "Admin email is required")
        @Email(message = "Valid admin email format is required")
        String adminEmail,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters long")
        String adminPassword,

        @Size(max = 20, message = "Admin mobile cannot exceed 20 characters")
        String adminMobile,

        @Size(max = 100, message = "Designation cannot exceed 100 characters")
        String adminDesignation
) {}
