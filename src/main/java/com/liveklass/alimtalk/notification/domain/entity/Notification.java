package com.liveklass.alimtalk.notification.domain.entity;

import com.liveklass.alimtalk.global.entity.BaseEntity;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * eventId + channel 조합이 발송 멱등성 키.
 * 동시 등록 요청은 이 유니크 제약(DB 레벨)이 최종 방어선.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_event_channel", columnNames = {"event_id", "channel"})
)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Lob
    @Column(name = "reference_data")
    private String referenceData;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    private Notification(String recipientId, NotificationType notificationType, String eventId,
                          String referenceData, NotificationChannel channel, LocalDateTime scheduledAt) {
        this.recipientId = recipientId;
        this.notificationType = notificationType;
        this.eventId = eventId;
        this.referenceData = referenceData;
        this.channel = channel;
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.read = false;
        this.scheduledAt = scheduledAt;
    }

    /**
     * scheduledAt이 null이면 즉시 발송 대상(현재 시각)으로 등록한다.
     */
    public static Notification createPending(String recipientId, NotificationType notificationType, String eventId,
                                               String referenceData, NotificationChannel channel,
                                               LocalDateTime scheduledAt) {
        return new Notification(recipientId, notificationType, eventId, referenceData, channel,
                scheduledAt != null ? scheduledAt : LocalDateTime.now());
    }

    public void markSuccess() {
        this.status = NotificationStatus.SUCCESS;
        this.failureReason = null;
    }

    /**
     * 백오프 없음 — 재시도 대상이면 바로 PENDING으로 돌려서 다음 워커 폴링에 즉시 잡히게 한다.
     */
    public void markFailure(String reason, int maxRetryCount) {
        this.retryCount++;
        this.failureReason = reason;
        this.status = (this.retryCount >= maxRetryCount) ? NotificationStatus.FAILED_FINAL : NotificationStatus.PENDING;
    }

    /**
     * 운영자가 최종 실패 건을 수동으로 재시도시킨다.
     * retryCount를 0으로 초기화한다 — 초기화하지 않으면 이미 maxRetryCount에 도달한 상태라
     * 재시도 1회 실패만으로 즉시 다시 FAILED_FINAL로 떨어져 "재시도"가 사실상 무의미해지기 때문.
     */
    public void retryManually() {
        this.status = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.scheduledAt = LocalDateTime.now();
    }
}