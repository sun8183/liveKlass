package com.liveklass.alimtalk.notification.dto.response;

import com.liveklass.alimtalk.notification.domain.entity.Notification;

import java.time.LocalDateTime;

public record NotificationRegisterResponse(
        Long notificationId,
        LocalDateTime scheduledAt
) {
    public static NotificationRegisterResponse from(Notification notification) {
        return new NotificationRegisterResponse(notification.getId(), notification.getScheduledAt());
    }
}