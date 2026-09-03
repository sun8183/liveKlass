package com.liveklass.alimtalk.global.security;

import com.liveklass.alimtalk.global.exception.BusinessException;
import com.liveklass.alimtalk.global.response.enums.CommonErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 운영자 전용 API(템플릿 관리 등) 앞단에 거는 최소 인증.
 * 회원용 알림 API와는 분리된 영역이라, 별도 인증 서버/RBAC 없이 API 키 헤더 검증으로 충분하다고 보고
 * 스코프를 이 정도로 한정했다. 실제 운영에서는 API Gateway나 별도 admin 인증으로 대체 가능.
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";

    @Value("${admin.api-key}")
    private String adminApiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String providedKey = request.getHeader(ADMIN_KEY_HEADER);
        if (providedKey == null || !providedKey.equals(adminApiKey)) {
            throw new BusinessException(CommonErrorStatus.UNAUTHORIZED);
        }
        return true;
    }
}
