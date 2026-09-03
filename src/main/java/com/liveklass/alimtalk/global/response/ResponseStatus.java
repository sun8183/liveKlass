package com.liveklass.alimtalk.global.response;

import org.springframework.http.HttpStatus;

public interface ResponseStatus {

    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}