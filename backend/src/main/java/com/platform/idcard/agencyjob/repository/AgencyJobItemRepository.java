package com.platform.idcard.agencyjob.repository;

import com.platform.idcard.agencyjob.entity.AgencyJobItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AgencyJobItemRepository extends JpaRepository<AgencyJobItem, UUID> {
    List<AgencyJobItem> findAllByAssignmentId(UUID assignmentId);
}
