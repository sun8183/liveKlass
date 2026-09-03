package com.liveklass.alimtalk.global.response;

public record InvalidFieldError(String field, String rejectedValue, String reason) {
}