package com.platform.idcard.student.repository;

import com.platform.idcard.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    boolean existsBySchoolIdAndAdmissionNumber(UUID schoolId, String admissionNumber);

    boolean existsBySchoolIdAndAdmissionNumberAndIdNot(UUID schoolId, String admissionNumber, UUID id);

    Optional<Student> findByIdAndSchoolId(UUID id, UUID schoolId);

    long countBySchoolId(UUID schoolId);

    Optional<Student> findBySchoolIdAndAdmissionNumber(UUID schoolId, String admissionNumber);

    Optional<Student> findBySchoolIdAndAdmissionNumberIgnoreCase(UUID schoolId, String admissionNumber);

    Optional<Student> findBySchoolIdAndClassNameIgnoreCaseAndSectionIgnoreCaseAndRollNumber(
            UUID schoolId, String className, String section, String rollNumber);

    java.util.List<Student> findBySchoolIdAndRollNumber(UUID schoolId, String rollNumber);

    boolean existsBySchoolIdAndClassNameIgnoreCaseAndSectionIgnoreCaseAndRollNumber(
            UUID schoolId, String className, String section, String rollNumber);

    @Query("SELECT s FROM Student s WHERE " +
           "s.school.id = :schoolId AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:className IS NULL OR s.className = :className) AND " +
           "(:section IS NULL OR s.section = :section) AND " +
           "(:search IS NULL OR LOWER(s.studentName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
           " LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
           " LOWER(s.rollNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Student> searchStudents(
            @Param("schoolId") UUID schoolId,
            @Param("search") String search,
            @Param("className") String className,
            @Param("section") String section,
            @Param("status") String status,
            Pageable pageable
    );
}
