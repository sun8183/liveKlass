package com.liveklass.alimtalk.global.response.enums;

import com.liveklass.alimtalk.global.response.ResponseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorStatus implements ResponseStatus {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "E001", "요청 값이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "E002", "요청하신 경로를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E003", "서버 내부 오류가 발생했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E004", "운영자 인증이 필요합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
