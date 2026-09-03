package com.liveklass.alimtalk.notification.scheduler;

import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.repository.NotificationRepository;
import com.liveklass.alimtalk.notification.service.NotificationClaimService;
import com.liveklass.alimtalk.notification.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 브로커 없이 DB 폴링으로 큐를 대체한다. 실제 브로커로 전환 시
 * 이 클래스(폴링해서 후보 id 뽑는 부분)만 컨슈머로 바뀌고,
 * NotificationDispatchService/NotificationSender는 그대로 재사용된다.
 */
@Component
@RequiredArgsConstructor
public class NotificationWorker {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRY_COUNT = 3;
    private static final Duration STUCK_THRESHOLD = Duration.ofMinutes(1);

    private final NotificationRepository notificationRepository;
    private final NotificationClaimService claimService;
    private final NotificationDispatchService dispatchService;

    @Scheduled(fixedDelay = 2000)
    public void dispatch() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime stuckBefore = now.minus(STUCK_THRESHOLD);
        List<Long> candidateIds = notificationRepository.findClaimableIds(
                NotificationStatus.PENDING, NotificationStatus.PROCESSING, now, stuckBefore,
                PageRequest.of(0, BATCH_SIZE));

        for (Long id : candidateIds) {
            if (claimService.claim(id, stuckBefore)) {
                dispatchService.processClaimed(id, MAX_RETRY_COUNT);
            }
        }
    }
}