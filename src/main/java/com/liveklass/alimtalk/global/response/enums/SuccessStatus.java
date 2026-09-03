package com.liveklass.alimtalk.global.response.enums;

import com.liveklass.alimtalk.global.response.ResponseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessStatus implements ResponseStatus {

    NOTIFICATION_REGISTERED(HttpStatus.ACCEPTED, "S001", "알림 발송 요청이 접수되었습니다."),
    NOTIFICATION_RETRIEVED(HttpStatus.OK, "S002", "알림 상태 조회에 성공했습니다."),
    NOTIFICATION_LIST_RETRIEVED(HttpStatus.OK, "S003", "알림 목록 조회에 성공했습니다."),
    NOTIFICATION_READ(HttpStatus.OK, "S004", "알림을 읽음 처리했습니다."),
    NOTIFICATION_RETRIED(HttpStatus.OK, "S005", "알림 재시도 요청이 접수되었습니다."),
    NOTIFICATION_FAILED_LIST_RETRIEVED(HttpStatus.OK, "S006", "최종 실패 알림 목록 조회에 성공했습니다."),
    NOTIFICATION_TEMPLATE_SAVED(HttpStatus.OK, "S007", "알림 템플릿을 저장했습니다."),
    NOTIFICATION_TEMPLATE_RETRIEVED(HttpStatus.OK, "S008", "알림 템플릿 조회에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}