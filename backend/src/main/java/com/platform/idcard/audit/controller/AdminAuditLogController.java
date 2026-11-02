package com.platform.idcard.audit.controller;

import com.platform.idcard.audit.dto.AuditLogResponse;
import com.platform.idcard.audit.service.AuditService;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Audit Logs", description = "Immutable system activity logs and compliance audit trail")
public class AdminAuditLogController {

    private final AuditService auditService;

    public AdminAuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "List audit logs", description = "Paginated list of system audit events with actor, entity, and action filters")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> listAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action
    ) {
        PagedResponse<AuditLogResponse> response = auditService.getAuditLogs(actorUserId, entityType, action, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
