package com.liveklass.alimtalk.global.exception;

import com.liveklass.alimtalk.global.response.ResponseStatus;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ResponseStatus status;

    public BusinessException(ResponseStatus status) {
        super(status.getMessage());
        this.status = status;
    }
}