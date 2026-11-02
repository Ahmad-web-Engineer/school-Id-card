package com.platform.idcard.audit.repository;

import com.platform.idcard.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:actorUserId IS NULL OR a.actor.id = :actorUserId) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:action IS NULL OR a.action = :action) " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLog> findWithFilters(
            @Param("actorUserId") UUID actorUserId,
            @Param("entityType") String entityType,
            @Param("action") String action,
            Pageable pageable
    );
}
