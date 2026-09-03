package com.liveklass.alimtalk.notification.service;

import com.liveklass.alimtalk.global.response.enums.ErrorStatus;
import com.liveklass.alimtalk.global.exception.BusinessException;
import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.dto.request.NotificationRegisterRequest;
import com.liveklass.alimtalk.notification.dto.response.NotificationDetailResponse;
import com.liveklass.alimtalk.notification.dto.response.NotificationRegisterResponse;
import com.liveklass.alimtalk.notification.dto.response.NotificationSummaryResponse;
import com.liveklass.alimtalk.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    /**
     * eventId+channel 이미 등록돼 있으면 BusinessException(409)으로 거절한다.
     * 동시 요청 레이스는 유니크 제약 위반을 잡아 같은 예외로 변환하는 방식으로 방어한다.
     */
    @Transactional
    public NotificationRegisterResponse register(NotificationRegisterRequest request) {
        notificationRepository.findByEventIdAndChannel(request.eventId(), request.channel())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorStatus.NOTIFICATION_ALREADY_REGISTERED);
                });

        Notification notification = Notification.createPending(
                request.recipientId(),
                request.notificationType(),
                request.eventId(),
                writeReferenceData(request.referenceData()),
                request.channel(),
                request.scheduledAt()
        );

        try {
            Notification saved = notificationRepository.save(notification);
            return NotificationRegisterResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorStatus.NOTIFICATION_ALREADY_REGISTERED);
        }
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.NOTIFICATION_NOT_FOUND));
        return NotificationDetailResponse.from(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationSummaryResponse> getNotifications(String recipientId, Boolean read, Pageable pageable) {
        Page<Notification> notifications = (read == null)
                ? notificationRepository.findByRecipientId(recipientId, pageable)
                : notificationRepository.findByRecipientIdAndRead(recipientId, read, pageable);
        return notifications.map(NotificationSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<NotificationSummaryResponse> getFailedNotifications(Pageable pageable) {
        return notificationRepository.findByStatus(NotificationStatus.FAILED_FINAL, pageable)
                .map(NotificationSummaryResponse::from);
    }

    /**
     * 여러 기기에서 동시에 읽음 처리를 요청해도 direct UPDATE라 순서 상관없이 최종 상태는 read=true로 수렴한다.
     */
    @Transactional
    public void markRead(Long notificationId) {
        int updated = notificationRepository.markRead(notificationId);
        if (updated == 0) {
            throw new BusinessException(ErrorStatus.NOTIFICATION_NOT_FOUND);
        }
    }

    /**
     * FAILED_FINAL 건만 수동 재시도 허용. retryCount 초기화 정책은 Notification.retryManually() 참고.
     */
    @Transactional
    public void retryManually(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorStatus.NOTIFICATION_NOT_FOUND));
        if (notification.getStatus() != NotificationStatus.FAILED_FINAL) {
            throw new BusinessException(ErrorStatus.NOTIFICATION_RETRY_NOT_ALLOWED);
        }
        notification.retryManually();
    }

    private String writeReferenceData(Map<String, Object> referenceData) {
        if (referenceData == null || referenceData.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(referenceData);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("referenceData 직렬화 실패", e);
        }
    }
}