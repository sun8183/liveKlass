package com.liveklass.alimtalk.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationStatus {

    PENDING("발송 대기"),
    PROCESSING("처리 중"),
    SUCCESS("발송 성공"),
    FAILED_FINAL("최종 실패");

    private final String description;
}