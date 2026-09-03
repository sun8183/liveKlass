package com.liveklass.alimtalk.notification.service;

import com.liveklass.alimtalk.global.exception.BusinessException;
import com.liveklass.alimtalk.global.response.enums.ErrorStatus;
import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.notification.dto.request.NotificationRegisterRequest;
import com.liveklass.alimtalk.notification.repository.NotificationRepository;
import com.liveklass.alimtalk.support.ConcurrencyTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationServiceConcurrencyTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void 같은_이벤트아이디와_채널로_동시등록하면_하나만_성공한다() throws InterruptedException {
        String eventId = "concurrency-register-" + UUID.randomUUID();
        NotificationRegisterRequest request = new NotificationRegisterRequest(
                "user-concurrency", NotificationType.ENROLLMENT_COMPLETED, eventId,
                null, NotificationChannel.IN_APP, null);

        int threadCount = 10;
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();

        ConcurrencyTestHelper.runConcurrently(threadCount, () -> {
            try {
                notificationService.register(request);
                successCount.incrementAndGet();
            } catch (BusinessException e) {
                if (e.getStatus() == ErrorStatus.NOTIFICATION_ALREADY_REGISTERED) {
                    conflictCount.incrementAndGet();
                } else {
                    unexpected.add(e);
                }
            } catch (Exception e) {
                unexpected.add(e);
            }
        });

        assertThat(unexpected).isEmpty();
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threadCount - 1);
        assertThat(notificationRepository.findByEventIdAndChannel(eventId, NotificationChannel.IN_APP)).isPresent();
    }

    @Test
    void 동시에_읽음처리해도_예외없이_읽음상태로_수렴한다() throws InterruptedException {
        Notification notification = Notification.createPending(
                "user-concurrency", NotificationType.ENROLLMENT_COMPLETED,
                "concurrency-read-" + UUID.randomUUID(), null, NotificationChannel.EMAIL, null);
        Long id = notificationRepository.save(notification).getId();

        int threadCount = 10;
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();

        ConcurrencyTestHelper.runConcurrently(threadCount, () -> {
            try {
                notificationService.markRead(id);
            } catch (Exception e) {
                unexpected.add(e);
            }
        });

        assertThat(unexpected).isEmpty();
        assertThat(notificationRepository.findById(id)).get()
                .extracting(Notification::isRead)
                .isEqualTo(true);
    }
}
