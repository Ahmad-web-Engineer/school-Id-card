package com.platform.idcard.student.repository;

import com.platform.idcard.student.entity.StudentImportError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentImportErrorRepository extends JpaRepository<StudentImportError, UUID> {

    List<StudentImportError> findByBatchIdOrderByRowNumberAsc(UUID batchId);
}
