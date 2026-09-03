package com.liveklass.alimtalk.global.response.enums;

import com.liveklass.alimtalk.global.response.ResponseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorStatus implements ResponseStatus {

    NOTIFICATION_ALREADY_REGISTERED(HttpStatus.CONFLICT, "N001", "이미 등록된 알림 요청입니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N002", "존재하지 않는 알림 요청입니다."),
    NOTIFICATION_RETRY_NOT_ALLOWED(HttpStatus.CONFLICT, "N003", "최종 실패 상태의 알림만 재시도할 수 있습니다."),
    NOTIFICATION_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "N004", "등록된 알림 템플릿이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}