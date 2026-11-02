package com.platform.idcard.student.repository;

import com.platform.idcard.student.entity.PhotoImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoImportBatchRepository extends JpaRepository<PhotoImportBatch, UUID> {

    Optional<PhotoImportBatch> findByIdAndSchoolId(UUID id, UUID schoolId);
}
