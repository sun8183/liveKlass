package com.liveklass.alimtalk.global.response;

import org.springframework.http.ResponseEntity;

public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ResponseEntity<ApiResponse<T>> of(ResponseStatus status, T data) {
        return ResponseEntity.status(status.getHttpStatus())
                .body(new ApiResponse<>(status.getCode(), status.getMessage(), data));
    }

    public static ResponseEntity<ApiResponse<Void>> of(ResponseStatus status) {
        return of(status, null);
    }
}