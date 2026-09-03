package com.liveklass.alimtalk.notification.service;

import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.repository.NotificationRepository;
import com.liveklass.alimtalk.notification.sender.NotificationSenderRegistry;
import com.liveklass.alimtalk.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * claim에 성공한(=소유권 확보한) id를 실제로 발송한다.
 * sender.send()는 이메일/푸시 등 외부 API 호출일 수 있어서 트랜잭션 밖에서 실행하고,
 * 결과 반영은 NotificationStateService의 짧은 트랜잭션에 위임한다.
 * (외부 호출을 트랜잭션 안에 두면 응답 대기 동안 DB 커넥션을 계속 붙잡게 되어 커넥션 풀 고갈로 번질 수 있음)
 * @Async로 배치 내 항목들을 병렬 처리한다(AsyncConfig 참고) — 순차처리면 배치처리시간이 B*T라
 * STUCK_THRESHOLD와 배치사이즈가 서로 근거상 얽히는데, 병렬처리로 그 커플링을 없앤다.
 */
@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationSenderRegistry senderRegistry;
    private final NotificationStateService stateService;
    private final TemplateService templateService;

    @Async
    public void processClaimed(Long notificationId, int maxRetryCount) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException("claim 직후 row가 사라짐: id=" + notificationId));

        try {
            String message = templateService.render(notification);
            senderRegistry.resolve(notification.getChannel()).send(notification, message);
            stateService.markSuccess(notificationId);
        } catch (Exception e) {
            log.warn("알림 발송 실패, id={}", notificationId, e);
            stateService.markFailure(notificationId, e.getMessage(), maxRetryCount);
        }
    }
}