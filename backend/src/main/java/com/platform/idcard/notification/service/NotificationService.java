package com.platform.idcard.notification.service;

import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.common.response.PaginationMeta;
import com.platform.idcard.notification.dto.MarkAllReadResponse;
import com.platform.idcard.notification.dto.NotificationResponse;
import com.platform.idcard.notification.dto.UnreadCountResponse;
import com.platform.idcard.notification.entity.Notification;
import com.platform.idcard.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public NotificationResponse sendNotification(
            UUID recipientUserId,
            String notificationType,
            String title,
            String message,
            String referenceType,
            UUID referenceId
    ) {
        User recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + recipientUserId));

        Notification notification = new Notification(
                recipient,
                notificationType,
                title,
                message,
                referenceType,
                referenceId
        );
        Notification saved = notificationRepository.save(notification);
        log.info("Notification [{}] sent to user [{}]", saved.getNotificationType(), recipient.getEmail());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUserNotifications(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadStatusOrderByCreatedAtDesc(userId, false, pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> data = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(data, meta);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countByRecipientIdAndReadStatus(userId, false);
        return new UnreadCountResponse(count);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found or access denied"));

        if (!notification.isReadStatus()) {
            notification.setReadStatus(true);
            notification.setReadAt(OffsetDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return mapToResponse(notification);
    }

    @Transactional
    public MarkAllReadResponse markAllAsRead(UUID userId) {
        int updated = notificationRepository.markAllAsRead(userId, OffsetDateTime.now());
        return new MarkAllReadResponse(updated);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getNotificationType(),
                n.getTitle(),
                n.getMessage(),
                n.getReferenceType(),
                n.getReferenceId(),
                n.isReadStatus(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}
