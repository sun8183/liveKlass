package com.liveklass.alimtalk.global.exception;

import com.liveklass.alimtalk.global.response.ApiResponse;
import com.liveklass.alimtalk.global.response.enums.CommonErrorStatus;
import com.liveklass.alimtalk.global.response.InvalidFieldError;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ApiResponse.of(e.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<InvalidFieldError>>> handleValidationException(MethodArgumentNotValidException e) {
        List<InvalidFieldError> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new InvalidFieldError(fe.getField(), String.valueOf(fe.getRejectedValue()), fe.getDefaultMessage()))
                .toList();
        return ApiResponse.of(CommonErrorStatus.INVALID_INPUT, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<List<InvalidFieldError>>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return ApiResponse.of(CommonErrorStatus.INVALID_INPUT, List.of(extractFieldError(e)));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<List<InvalidFieldError>>> handleMissingParameter(MissingServletRequestParameterException e) {
        InvalidFieldError error = new InvalidFieldError(e.getParameterName(), null, "필수 파라미터입니다.");
        return ApiResponse.of(CommonErrorStatus.INVALID_INPUT, List.of(error));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<List<InvalidFieldError>>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        Class<?> requiredType = e.getRequiredType();
        String reason = "값의 형식이 올바르지 않습니다." + (requiredType != null ? " (" + requiredType.getSimpleName() + " 필요)" : "");
        InvalidFieldError error = new InvalidFieldError(e.getName(), String.valueOf(e.getValue()), reason);
        return ApiResponse.of(CommonErrorStatus.INVALID_INPUT, List.of(error));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
        return ApiResponse.of(CommonErrorStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ApiResponse.of(CommonErrorStatus.INTERNAL_SERVER_ERROR);
    }

    private InvalidFieldError extractFieldError(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof InvalidFormatException ife) {
            String field = ife.getPath().stream()
                    .map(tools.jackson.core.JacksonException.Reference::getPropertyName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("unknown");
            Class<?> targetType = ife.getTargetType();
            String reason = (targetType != null && targetType.isEnum())
                    ? "허용되지 않는 값입니다. 가능한 값: " + Arrays.toString(targetType.getEnumConstants())
                    : "값의 형식이 올바르지 않습니다.";
            return new InvalidFieldError(field, String.valueOf(ife.getValue()), reason);
        }
        return new InvalidFieldError("unknown", null, "요청 본문을 읽을 수 없습니다.");
    }
}