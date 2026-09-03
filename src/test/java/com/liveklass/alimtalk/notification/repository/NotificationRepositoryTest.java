package com.liveklass.alimtalk.notification.repository;

import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void 발송시각이_지난_대기건만_포함하고_미래예약건은_제외한다() {
        LocalDateTime now = LocalDateTime.now();
        Long dueId = save(NotificationStatus.PENDING, now.minusMinutes(1)).getId();
        Long futureId = save(NotificationStatus.PENDING, now.plusMinutes(10)).getId();

        List<Long> ids = notificationRepository.findClaimableIds(
                NotificationStatus.PENDING, NotificationStatus.PROCESSING, now, now.minusMinutes(1),
                PageRequest.of(0, 50));

        assertThat(ids).contains(dueId);
        assertThat(ids).doesNotContain(futureId);
    }

    @Test
    void 오래된_처리중건은_포함하고_최근_처리중건은_제외한다() {
        LocalDateTime now = LocalDateTime.now();
        Long stuckId = save(NotificationStatus.PROCESSING, now).getId();
        Long freshId = save(NotificationStatus.PROCESSING, now).getId();
        backdateUpdatedAt(stuckId, now.minusMinutes(5));

        List<Long> ids = notificationRepository.findClaimableIds(
                NotificationStatus.PENDING, NotificationStatus.PROCESSING, now, now.minusMinutes(1),
                PageRequest.of(0, 50));

        assertThat(ids).contains(stuckId);
        assertThat(ids).doesNotContain(freshId);
    }

    @Test
    void 대기중인_알림을_claim하면_처리중으로_바뀌고_1을_반환한다() {
        Long id = save(NotificationStatus.PENDING, LocalDateTime.now()).getId();

        int claimed = notificationRepository.claim(id, NotificationStatus.PENDING, NotificationStatus.PROCESSING,
                LocalDateTime.now().minusMinutes(1));

        assertThat(claimed).isEqualTo(1);
        entityManager.clear();
        assertThat(notificationRepository.findById(id)).get()
                .extracting(Notification::getStatus)
                .isEqualTo(NotificationStatus.PROCESSING);
    }

    @Test
    void 최근_처리중인_알림은_claim되지_않고_0을_반환한다() {
        Long id = save(NotificationStatus.PROCESSING, LocalDateTime.now()).getId();

        int claimed = notificationRepository.claim(id, NotificationStatus.PENDING, NotificationStatus.PROCESSING,
                LocalDateTime.now().minusMinutes(1));

        assertThat(claimed).isEqualTo(0);
    }

    @Test
    void 오래된_처리중_알림은_다시_claim되고_1을_반환한다() {
        Long id = save(NotificationStatus.PROCESSING, LocalDateTime.now()).getId();
        backdateUpdatedAt(id, LocalDateTime.now().minusMinutes(5));

        int claimed = notificationRepository.claim(id, NotificationStatus.PENDING, NotificationStatus.PROCESSING,
                LocalDateTime.now().minusMinutes(1));

        assertThat(claimed).isEqualTo(1);
    }

    @Test
    void 존재하는_알림을_읽음처리하면_1을_반환한다() {
        Long id = save(NotificationStatus.PENDING, LocalDateTime.now()).getId();

        int updated = notificationRepository.markRead(id);

        assertThat(updated).isEqualTo(1);
        entityManager.clear();
        assertThat(notificationRepository.findById(id)).get()
                .extracting(Notification::isRead)
                .isEqualTo(true);
    }

    @Test
    void 존재하지_않는_알림을_읽음처리하면_0을_반환한다() {
        int updated = notificationRepository.markRead(-1L);

        assertThat(updated).isEqualTo(0);
    }

    @Test
    void 이벤트아이디와_채널이_정확히_일치할때만_조회된다() {
        String eventId = "repo-test-" + UUID.randomUUID();
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, eventId, null, NotificationChannel.EMAIL, null);
        notificationRepository.save(notification);

        assertThat(notificationRepository.findByEventIdAndChannel(eventId, NotificationChannel.EMAIL)).isPresent();
        assertThat(notificationRepository.findByEventIdAndChannel(eventId, NotificationChannel.IN_APP)).isEmpty();
    }

    private Notification save(NotificationStatus status, LocalDateTime scheduledAt) {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "repo-test-" + UUID.randomUUID(),
                null, NotificationChannel.EMAIL, scheduledAt);
        Notification saved = notificationRepository.save(notification);
        if (status == NotificationStatus.PROCESSING) {
            entityManager.createQuery("update Notification n set n.status = :status where n.id = :id")
                    .setParameter("status", status)
                    .setParameter("id", saved.getId())
                    .executeUpdate();
            entityManager.clear();
        }
        return saved;
    }

    private void backdateUpdatedAt(Long id, LocalDateTime time) {
        entityManager.createQuery("update Notification n set n.updatedAt = :time where n.id = :id")
                .setParameter("time", time)
                .setParameter("id", id)
                .executeUpdate();
        entityManager.clear();
    }
}
