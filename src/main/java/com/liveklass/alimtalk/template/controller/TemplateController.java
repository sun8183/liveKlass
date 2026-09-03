package com.liveklass.alimtalk.template.controller;

import com.liveklass.alimtalk.global.response.ApiResponse;
import com.liveklass.alimtalk.global.response.enums.SuccessStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.template.dto.request.TemplateRequest;
import com.liveklass.alimtalk.template.dto.response.TemplateResponse;
import com.liveklass.alimtalk.template.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification-templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PutMapping("/{notificationType}")
    public ResponseEntity<ApiResponse<TemplateResponse>> upsert(
            @PathVariable NotificationType notificationType,
            @Valid @RequestBody TemplateRequest request) {
        return ApiResponse.of(SuccessStatus.NOTIFICATION_TEMPLATE_SAVED, templateService.upsert(notificationType, request));
    }

    @GetMapping("/{notificationType}")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(@PathVariable NotificationType notificationType) {
        return ApiResponse.of(SuccessStatus.NOTIFICATION_TEMPLATE_RETRIEVED, templateService.getTemplate(notificationType));
    }
}
