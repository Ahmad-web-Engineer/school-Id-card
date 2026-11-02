package com.platform.idcard.student.dto;

import com.platform.idcard.student.entity.Student;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        UUID schoolId,
        String admissionNumber,
        String studentName,
        String fatherName,
        String motherName,
        LocalDate dateOfBirth,
        String gender,
        String bloodGroup,
        String className,
        String section,
        String rollNumber,
        String address,
        String mobile,
        String emergencyContact,
        String photoFileId,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static StudentResponse fromEntity(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getSchool().getId(),
                student.getAdmissionNumber(),
                student.getStudentName(),
                student.getFatherName(),
                student.getMotherName(),
                student.getDateOfBirth(),
                student.getGender(),
                student.getBloodGroup(),
                student.getClassName(),
                student.getSection(),
                student.getRollNumber(),
                student.getAddress(),
                student.getMobile(),
                student.getEmergencyContact(),
                student.getPhotoFileId() != null ? student.getPhotoFileId().toString() : null,
                student.getStatus(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }
}
