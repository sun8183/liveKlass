package com.liveklass.alimtalk.notification.sender;

import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NotificationSenderRegistry {

    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationSenderRegistry(List<NotificationSender> senders) {
        this.senders = senders.stream()
                .collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
    }

    public NotificationSender resolve(NotificationChannel channel) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            throw new IllegalStateException("등록된 발송기가 없습니다: " + channel);
        }
        return sender;
    }
}