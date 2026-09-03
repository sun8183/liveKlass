package com.liveklass.alimtalk.notification.dto.response;

import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationDetailResponse(
        Long notificationId,
        String recipientId,
        NotificationType notificationType,
        NotificationChannel channel,
        NotificationStatus status,
        String statusDescription,
        String failureReason,
        int retryCount,
        boolean read,
        LocalDateTime scheduledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NotificationDetailResponse from(Notification notification) {
        return new NotificationDetailResponse(
                notification.getId(),
                notification.getRecipientId(),
                notification.getNotificationType(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getStatus().getDescription(),
                notification.getFailureReason(),
                notification.getRetryCount(),
                notification.isRead(),
                notification.getScheduledAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}