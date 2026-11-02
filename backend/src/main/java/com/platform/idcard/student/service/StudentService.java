package com.platform.idcard.student.service;

import com.platform.idcard.common.exception.BusinessConflictException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.common.response.PaginationMeta;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.organization.repository.SchoolRepository;
import com.platform.idcard.student.dto.StudentRequest;
import com.platform.idcard.student.dto.StudentResponse;
import com.platform.idcard.student.entity.Student;
import com.platform.idcard.student.repository.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;

    public StudentService(StudentRepository studentRepository, SchoolRepository schoolRepository) {
        this.studentRepository = studentRepository;
        this.schoolRepository = schoolRepository;
    }

    public PagedResponse<StudentResponse> getStudents(
            UUID schoolId,
            int page,
            int size,
            String search,
            String className,
            String section,
            String status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("className").ascending().and(Sort.by("section").ascending()).and(Sort.by("rollNumber").ascending()));

        String searchTrimmed = (search != null && !search.isBlank()) ? search.trim() : null;
        String classTrimmed = (className != null && !className.isBlank()) ? className.trim() : null;
        String sectionTrimmed = (section != null && !section.isBlank()) ? section.trim().toUpperCase() : null;
        String statusTrimmed = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        Page<Student> pageResult = studentRepository.searchStudents(
                schoolId,
                searchTrimmed,
                classTrimmed,
                sectionTrimmed,
                statusTrimmed,
                pageable
        );

        var data = pageResult.getContent().stream()
                .map(StudentResponse::fromEntity)
                .toList();

        PaginationMeta meta = PaginationMeta.of(
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );

        return PagedResponse.of(data, meta);
    }

    @Transactional
    public StudentResponse createStudent(UUID schoolId, StudentRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School with ID " + schoolId + " not found."));

        String admissionNumber = request.admissionNumber().trim();
        if (studentRepository.existsBySchoolIdAndAdmissionNumber(schoolId, admissionNumber)) {
            throw new BusinessConflictException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Student with admission number '" + admissionNumber + "' already exists in this school."
            );
        }

        Student student = new Student();
        student.setSchool(school);
        student.setAdmissionNumber(admissionNumber);
        student.setStudentName(request.studentName().trim());
        student.setFatherName(request.fatherName() != null ? request.fatherName().trim() : null);
        student.setMotherName(request.motherName() != null ? request.motherName().trim() : null);
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender() != null && !request.gender().isBlank() ? request.gender().trim().toUpperCase() : null);
        student.setBloodGroup(request.bloodGroup() != null && !request.bloodGroup().isBlank() ? request.bloodGroup().trim().toUpperCase() : null);
        student.setClassName(request.className().trim());
        student.setSection(request.section() != null ? request.section().trim().toUpperCase() : null);
        student.setRollNumber(request.rollNumber() != null ? request.rollNumber().trim() : null);
        student.setAddress(request.address() != null ? request.address().trim() : null);
        student.setMobile(request.mobile() != null ? request.mobile().trim() : null);
        student.setEmergencyContact(request.emergencyContact() != null ? request.emergencyContact().trim() : null);
        student.setPhotoFileId(request.photoFileId());
        student.setStatus(request.status() != null && !request.status().isBlank() ? request.status().trim().toUpperCase() : "ACTIVE");

        Student saved = studentRepository.save(student);
        return StudentResponse.fromEntity(saved);
    }

    public StudentResponse getStudentById(UUID schoolId, UUID studentId) {
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        return StudentResponse.fromEntity(student);
    }

    @Transactional
    public StudentResponse updateStudent(UUID schoolId, UUID studentId, StudentRequest request) {
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));

        String newAdmissionNumber = request.admissionNumber().trim();
        if (studentRepository.existsBySchoolIdAndAdmissionNumberAndIdNot(schoolId, newAdmissionNumber, studentId)) {
            throw new BusinessConflictException(
                    ErrorCode.RESOURCE_CONFLICT,
                    "Student with admission number '" + newAdmissionNumber + "' already exists in this school."
            );
        }

        student.setAdmissionNumber(newAdmissionNumber);
        student.setStudentName(request.studentName().trim());
        student.setFatherName(request.fatherName() != null ? request.fatherName().trim() : null);
        student.setMotherName(request.motherName() != null ? request.motherName().trim() : null);
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender() != null && !request.gender().isBlank() ? request.gender().trim().toUpperCase() : null);
        student.setBloodGroup(request.bloodGroup() != null && !request.bloodGroup().isBlank() ? request.bloodGroup().trim().toUpperCase() : null);
        student.setClassName(request.className().trim());
        student.setSection(request.section() != null ? request.section().trim().toUpperCase() : null);
        student.setRollNumber(request.rollNumber() != null ? request.rollNumber().trim() : null);
        student.setAddress(request.address() != null ? request.address().trim() : null);
        student.setMobile(request.mobile() != null ? request.mobile().trim() : null);
        student.setEmergencyContact(request.emergencyContact() != null ? request.emergencyContact().trim() : null);
        student.setPhotoFileId(request.photoFileId());
        if (request.status() != null && !request.status().isBlank()) {
            student.setStatus(request.status().trim().toUpperCase());
        }

        Student updated = studentRepository.save(student);
        return StudentResponse.fromEntity(updated);
    }

    @Transactional
    public void deactivateStudent(UUID schoolId, UUID studentId) {
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found."));
        student.setStatus("INACTIVE");
        studentRepository.save(student);
    }
}
