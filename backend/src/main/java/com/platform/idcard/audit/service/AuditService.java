package com.platform.idcard.audit.service;

import com.platform.idcard.audit.dto.AuditLogResponse;
import com.platform.idcard.audit.entity.AuditLog;
import com.platform.idcard.audit.repository.AuditLogRepository;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.common.response.PaginationMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog recordAudit(
            UUID actorUserId,
            String actorName,
            String actorRole,
            String action,
            String entityType,
            String entityId,
            String details,
            String ipAddress
    ) {
        User actor = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;
        AuditLog auditLog = new AuditLog(
                actor,
                actorName != null ? actorName : (actor != null ? actor.getFullName() : "SYSTEM"),
                actorRole,
                action,
                entityType,
                entityId,
                details,
                ipAddress
        );
        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit: user [{}] performed [{}] on [{}:{}]", saved.getActorName(), action, entityType, entityId);
        return saved;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getAuditLogs(
            UUID actorUserId,
            String entityType,
            String action,
            Pageable pageable
    ) {
        Page<AuditLog> page = auditLogRepository.findWithFilters(actorUserId, entityType, action, pageable);
        List<AuditLogResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    private AuditLogResponse mapToResponse(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getActor() != null ? a.getActor().getId() : null,
                a.getActorName(),
                a.getActorRole(),
                a.getAction(),
                a.getEntityType(),
                a.getEntityId(),
                a.getDetails(),
                a.getIpAddress(),
                a.getCreatedAt()
        );
    }
}
