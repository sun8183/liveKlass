package com.liveklass.alimtalk.notification.service;

import com.liveklass.alimtalk.global.exception.BusinessException;
import com.liveklass.alimtalk.global.response.enums.ErrorStatus;
import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.notification.dto.request.NotificationRegisterRequest;
import com.liveklass.alimtalk.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, new JsonMapper());
    }

    @Test
    void 같은_이벤트아이디와_채널로_등록하면_예외가_발생한다() {
        NotificationRegisterRequest request = new NotificationRegisterRequest(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);
        Notification existing = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);
        given(notificationRepository.findByEventIdAndChannel("evt-1", NotificationChannel.EMAIL))
                .willReturn(Optional.of(existing));

        assertThatThrownBy(() -> notificationService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(ErrorStatus.NOTIFICATION_ALREADY_REGISTERED);
    }

    @Test
    void 존재하지_않는_알림을_조회하면_예외가_발생한다() {
        given(notificationRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotification(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(ErrorStatus.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void 읽음여부_지정없으면_수신자기준_전체조회를_사용한다() {
        Pageable pageable = Pageable.unpaged();
        given(notificationRepository.findByRecipientId(eq("user-1"), any())).willReturn(Page.empty());

        notificationService.getNotifications("user-1", null, pageable);

        verify(notificationRepository).findByRecipientId(eq("user-1"), any());
    }

    @Test
    void 읽음여부_지정하면_읽음필터_조회를_사용한다() {
        Pageable pageable = Pageable.unpaged();
        given(notificationRepository.findByRecipientIdAndRead(eq("user-1"), eq(true), any())).willReturn(Page.empty());

        notificationService.getNotifications("user-1", true, pageable);

        verify(notificationRepository).findByRecipientIdAndRead(eq("user-1"), eq(true), any());
    }

    @Test
    void 최종실패_목록조회는_최종실패_상태로_조회한다() {
        Pageable pageable = Pageable.unpaged();
        given(notificationRepository.findByStatus(eq(NotificationStatus.FAILED_FINAL), any())).willReturn(Page.empty());

        notificationService.getFailedNotifications(pageable);

        verify(notificationRepository).findByStatus(eq(NotificationStatus.FAILED_FINAL), any());
    }

    @Test
    void 존재하지_않는_알림을_읽음처리하면_예외가_발생한다() {
        given(notificationRepository.markRead(1L)).willReturn(0);

        assertThatThrownBy(() -> notificationService.markRead(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(ErrorStatus.NOTIFICATION_NOT_FOUND);
    }

    @Test
    void 존재하는_알림을_읽음처리하면_예외가_발생하지_않는다() {
        given(notificationRepository.markRead(1L)).willReturn(1);

        notificationService.markRead(1L);

        verify(notificationRepository).markRead(1L);
    }

    @Test
    void 최종실패가_아니면_수동재시도시_예외가_발생한다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.retryManually(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(ErrorStatus.NOTIFICATION_RETRY_NOT_ALLOWED);
    }

    @Test
    void 최종실패상태면_수동재시도시_대기상태로_초기화된다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);
        notification.markFailure("err1", 1);
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        notificationService.retryManually(1L);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getRetryCount()).isEqualTo(0);
    }
}
