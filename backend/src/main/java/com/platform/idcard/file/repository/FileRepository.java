package com.platform.idcard.file.repository;

import com.platform.idcard.file.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    Optional<FileEntity> findByIdAndSchoolId(UUID id, UUID schoolId);
}
