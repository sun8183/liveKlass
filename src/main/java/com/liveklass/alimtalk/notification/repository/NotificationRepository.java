package com.liveklass.alimtalk.notification.repository;

import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByEventIdAndChannel(String eventId, NotificationChannel channel);

    Page<Notification> findByRecipientId(String recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndRead(String recipientId, boolean read, Pageable pageable);

    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    /**
     * 발송 시각이 도래한 대기(PENDING) 건, 또는 stuck된 PROCESSING(끊긴 워커가 물고 있던 것) row의 id만 뽑는다.
     */
    @Query("select n.id from Notification n " +
            "where (n.status = :pending and n.scheduledAt <= :now) " +
            "or (n.status = :processing and n.updatedAt < :stuckBefore) " +
            "order by n.createdAt asc")
    List<Long> findClaimableIds(@Param("pending") NotificationStatus pending,
                                 @Param("processing") NotificationStatus processing,
                                 @Param("now") LocalDateTime now,
                                 @Param("stuckBefore") LocalDateTime stuckBefore,
                                 Pageable pageable);

    /**
     * 읽음 처리는 단조(monotonic) 연산 — 두 번 이상 true로 세팅해도 결과가 같으므로
     * 여러 기기에서 동시에 요청이 와도 read-then-write 경합(lost update) 없이 그냥 직접 SET으로 처리한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.id = :id")
    int markRead(@Param("id") Long id);

    /**
     * 원자적 claim. 영향받은 row 수가 1이면 이 인스턴스가 소유권 획득, 0이면 다른 인스턴스가 이미 가져간 것.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.status = :processing, n.updatedAt = CURRENT_TIMESTAMP " +
            "where n.id = :id and (n.status = :pending or (n.status = :processing and n.updatedAt < :stuckBefore))")
    int claim(@Param("id") Long id,
              @Param("pending") NotificationStatus pending,
              @Param("processing") NotificationStatus processing,
              @Param("stuckBefore") LocalDateTime stuckBefore);
}