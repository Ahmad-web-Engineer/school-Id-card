package com.platform.idcard.student.repository;

import com.platform.idcard.student.entity.PhotoImportResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PhotoImportResultRepository extends JpaRepository<PhotoImportResult, UUID> {

    List<PhotoImportResult> findByBatchIdOrderByIdAsc(UUID batchId);
}
