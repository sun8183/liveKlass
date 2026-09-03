package com.liveklass.alimtalk.notification.service;

import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * claim을 발송/상태전이와 분리된 짧은 트랜잭션으로 즉시 커밋한다.
 * 이래야 PROCESSING 전이가 다른 인스턴스에도 바로 보이고, stuck 복구(오래 PROCESSING인 것 회수)가 실제로 의미를 가진다.
 * (claim과 발송을 한 트랜잭션에 묶으면, 발송 도중 죽었을 때 커밋 자체가 안 돼서 그냥 PENDING으로 롤백되고 stuck 상황이 생길 수가 없다.)
 */
@Service
@RequiredArgsConstructor
public class NotificationClaimService {

    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claim(Long notificationId, LocalDateTime stuckBefore) {
        int claimed = notificationRepository.claim(
                notificationId, NotificationStatus.PENDING, NotificationStatus.PROCESSING, stuckBefore);
        return claimed == 1;
    }
}