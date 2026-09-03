package com.liveklass.alimtalk.notification.dto.response;

import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationSummaryResponse(
        Long notificationId,
        NotificationType notificationType,
        NotificationChannel channel,
        NotificationStatus status,
        boolean read,
        LocalDateTime scheduledAt,
        LocalDateTime createdAt
) {
    public static NotificationSummaryResponse from(Notification notification) {
        return new NotificationSummaryResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getChannel(),
                notification.getStatus(),
                notification.isRead(),
                notification.getScheduledAt(),
                notification.getCreatedAt()
        );
    }
}