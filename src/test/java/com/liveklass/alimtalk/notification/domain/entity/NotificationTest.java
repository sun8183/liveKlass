package com.liveklass.alimtalk.notification.domain.entity;

import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    @Test
    void 예약시각_없으면_현재시각으로_설정된다() {
        LocalDateTime before = LocalDateTime.now();

        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);

        assertThat(notification.getScheduledAt()).isAfterOrEqualTo(before);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getRetryCount()).isEqualTo(0);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void 예약시각_있으면_그값을_사용한다() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(1);

        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, scheduledAt);

        assertThat(notification.getScheduledAt()).isEqualTo(scheduledAt);
    }

    @Test
    void 발송성공_처리하면_상태가_SUCCESS이고_실패사유가_지워진다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);
        notification.markFailure("temporary error", 3);

        notification.markSuccess();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SUCCESS);
        assertThat(notification.getFailureReason()).isNull();
    }

    @Test
    void 최대재시도_미만이면_대기상태로_돌아가고_재시도횟수가_증가한다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);

        notification.markFailure("network timeout", 3);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getRetryCount()).isEqualTo(1);
        assertThat(notification.getFailureReason()).isEqualTo("network timeout");
    }

    @Test
    void 최대재시도_도달하면_최종실패로_바뀐다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);

        notification.markFailure("err1", 2);
        notification.markFailure("err2", 2);

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED_FINAL);
        assertThat(notification.getRetryCount()).isEqualTo(2);
    }

    @Test
    void 수동재시도하면_재시도횟수가_초기화되고_즉시_재예약된다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);
        notification.markFailure("err1", 1);
        LocalDateTime before = LocalDateTime.now();

        notification.retryManually();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getRetryCount()).isEqualTo(0);
        assertThat(notification.getScheduledAt()).isAfterOrEqualTo(before);
    }
}
