package com.platform.idcard.student.repository;

import com.platform.idcard.student.entity.StudentImportRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentImportRowRepository extends JpaRepository<StudentImportRow, UUID> {

    List<StudentImportRow> findByBatchIdOrderByRowNumberAsc(UUID batchId);

    Page<StudentImportRow> findByBatchId(UUID batchId, Pageable pageable);

    Page<StudentImportRow> findByBatchIdAndValidationStatus(UUID batchId, String validationStatus, Pageable pageable);

    List<StudentImportRow> findByBatchIdAndValidationStatus(UUID batchId, String validationStatus);

    long countByBatchIdAndValidationStatus(UUID batchId, String validationStatus);
}
