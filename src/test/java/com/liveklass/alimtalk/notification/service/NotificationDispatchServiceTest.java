package com.liveklass.alimtalk.notification.service;

import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.notification.repository.NotificationRepository;
import com.liveklass.alimtalk.notification.sender.NotificationSender;
import com.liveklass.alimtalk.notification.sender.NotificationSenderRegistry;
import com.liveklass.alimtalk.template.service.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationSenderRegistry senderRegistry;

    @Mock
    private NotificationStateService stateService;

    @Mock
    private TemplateService templateService;

    @Mock
    private NotificationSender sender;

    private NotificationDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new NotificationDispatchService(notificationRepository, senderRegistry, stateService, templateService);
    }

    @Test
    void 발송성공하면_성공으로_기록한다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));
        given(templateService.render(notification)).willReturn("hello");
        given(senderRegistry.resolve(NotificationChannel.EMAIL)).willReturn(sender);

        dispatchService.processClaimed(1L, 3);

        verify(sender).send(notification, "hello");
        verify(stateService).markSuccess(1L);
        verify(stateService, never()).markFailure(any(), any(), anyInt());
    }

    @Test
    void 발송실패하면_실패사유와_함께_실패로_기록한다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-2", null, NotificationChannel.EMAIL, null);
        given(notificationRepository.findById(2L)).willReturn(Optional.of(notification));
        given(templateService.render(notification)).willReturn("hello");
        given(senderRegistry.resolve(NotificationChannel.EMAIL)).willReturn(sender);
        doThrow(new RuntimeException("boom")).when(sender).send(notification, "hello");

        dispatchService.processClaimed(2L, 3);

        verify(stateService).markFailure(2L, "boom", 3);
        verify(stateService, never()).markSuccess(any());
    }

    @Test
    void 대상알림이_없으면_예외가_발생한다() {
        given(notificationRepository.findById(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> dispatchService.processClaimed(3L, 3))
                .isInstanceOf(IllegalStateException.class);
    }
}
