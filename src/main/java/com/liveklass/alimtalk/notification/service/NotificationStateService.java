package com.liveklass.alimtalk.notification.service;

import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발송 결과(성공/실패) 반영만 짧은 트랜잭션으로 커밋한다.
 * 외부 API 호출(NotificationDispatchService의 sender.send())과 분리해서,
 * 네트워크 대기 시간 동안 DB 커넥션/트랜잭션을 붙잡지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class NotificationStateService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void markSuccess(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException("발송 결과 반영 중 row가 사라짐: id=" + notificationId));
        notification.markSuccess();
    }

    @Transactional
    public void markFailure(Long notificationId, String reason, int maxRetryCount) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalStateException("발송 결과 반영 중 row가 사라짐: id=" + notificationId));
        notification.markFailure(reason, maxRetryCount);
    }
}