package com.platform.idcard.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StudentRequest(
        @NotBlank(message = "Admission number is required")
        @Size(max = 50, message = "Admission number cannot exceed 50 characters")
        String admissionNumber,

        @NotBlank(message = "Student name is required")
        @Size(max = 150, message = "Student name cannot exceed 150 characters")
        String studentName,

        @Size(max = 150, message = "Father name cannot exceed 150 characters")
        String fatherName,

        @Size(max = 150, message = "Mother name cannot exceed 150 characters")
        String motherName,

        LocalDate dateOfBirth,

        @Pattern(regexp = "^(MALE|FEMALE|OTHER|)$", message = "Gender must be MALE, FEMALE, or OTHER")
        String gender,

        @Pattern(regexp = "^(A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-|)$", message = "Valid blood group required (e.g. A+, B+, O+, AB-)")
        String bloodGroup,

        @NotBlank(message = "Class name is required")
        @Size(max = 50, message = "Class name cannot exceed 50 characters")
        String className,

        @Size(max = 20, message = "Section cannot exceed 20 characters")
        String section,

        @Size(max = 30, message = "Roll number cannot exceed 30 characters")
        String rollNumber,

        @Size(max = 300, message = "Address cannot exceed 300 characters")
        String address,

        @Size(max = 20, message = "Mobile cannot exceed 20 characters")
        String mobile,

        @Size(max = 20, message = "Emergency contact cannot exceed 20 characters")
        String emergencyContact,

        @Size(max = 255, message = "Photo file identifier cannot exceed 255 characters")
        String photoFileId,

        String status
) {}
