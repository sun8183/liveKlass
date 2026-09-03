package com.liveklass.alimtalk.notification.service;

import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.notification.repository.NotificationRepository;
import com.liveklass.alimtalk.support.ConcurrencyTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationClaimServiceConcurrencyTest {

    @Autowired
    private NotificationClaimService claimService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void 같은_알림을_동시에_claim하면_하나만_성공한다() throws InterruptedException {
        Notification notification = Notification.createPending(
                "user-concurrency", NotificationType.PAYMENT_CONFIRMED,
                "concurrency-claim-" + UUID.randomUUID(), null, NotificationChannel.EMAIL, null);
        Long id = notificationRepository.save(notification).getId();
        LocalDateTime stuckBefore = LocalDateTime.now().minusMinutes(1);

        int threadCount = 10;
        AtomicInteger claimedCount = new AtomicInteger();
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();

        ConcurrencyTestHelper.runConcurrently(threadCount, () -> {
            try {
                if (claimService.claim(id, stuckBefore)) {
                    claimedCount.incrementAndGet();
                }
            } catch (Exception e) {
                unexpected.add(e);
            }
        });

        assertThat(unexpected).isEmpty();
        assertThat(claimedCount.get()).isEqualTo(1);
        assertThat(notificationRepository.findById(id)).get()
                .extracting(Notification::getStatus)
                .isEqualTo(NotificationStatus.PROCESSING);
    }
}
