package com.liveklass.alimtalk.notification.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    ENROLLMENT_COMPLETED("수강 신청 완료"),
    PAYMENT_CONFIRMED("결제 확정"),
    LECTURE_STARTING_SOON("강의 시작 임박 (D-1)"),
    ENROLLMENT_CANCELLED("수강 취소");

    private final String description;
}