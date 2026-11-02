package com.platform.idcard.student.repository;

import com.platform.idcard.student.entity.StudentImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentImportBatchRepository extends JpaRepository<StudentImportBatch, UUID> {

    Optional<StudentImportBatch> findByIdAndSchoolId(UUID id, UUID schoolId);
}
