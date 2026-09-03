package com.liveklass.alimtalk.notification.sender;

import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;

/**
 * 채널별 실제 발송 어댑터. Mock 구현을 실제 SMTP/FCM 등으로 교체해도
 * NotificationDispatchService 쪽 오케스트레이션 로직은 바뀔 필요 없다.
 */
public interface NotificationSender {

    NotificationChannel channel();

    void send(Notification notification, String message);
}