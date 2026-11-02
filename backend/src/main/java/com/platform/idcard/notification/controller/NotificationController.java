package com.platform.idcard.notification.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.notification.dto.MarkAllReadResponse;
import com.platform.idcard.notification.dto.NotificationResponse;
import com.platform.idcard.notification.dto.UnreadCountResponse;
import com.platform.idcard.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notifications", description = "User notifications, unread counts, and read status management")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List user notifications", description = "Paginated list of notifications for the logged-in user")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> listNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PagedResponse<NotificationResponse> response = notificationService.getUserNotifications(userId, unreadOnly, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Returns number of unread notifications for badge indicators")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UnreadCountResponse response = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark as read", description = "Marks a specific notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        NotificationResponse response = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Marks all unread notifications as read for current user")
    public ResponseEntity<ApiResponse<MarkAllReadResponse>> markAllAsRead() {
        UUID userId = SecurityUtils.getCurrentUserId();
        MarkAllReadResponse response = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
