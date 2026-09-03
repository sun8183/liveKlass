package com.liveklass.alimtalk.notification.dto.request;

import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * scheduledAt 비워두면 즉시 발송 대상으로 등록된다.
 */
public record NotificationRegisterRequest(
        @NotBlank String recipientId,
        @NotNull NotificationType notificationType,
        @NotBlank String eventId,
        Map<String, Object> referenceData,
        @NotNull NotificationChannel channel,
        @Future LocalDateTime scheduledAt
) {
}