package com.liveklass.alimtalk.notification.sender;

import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockEmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(MockEmailNotificationSender.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification, String message) {
        log.info("[MOCK EMAIL] to={}, type={}, eventId={}, message={}",
                notification.getRecipientId(), notification.getNotificationType(),
                notification.getEventId(), message);
    }
}