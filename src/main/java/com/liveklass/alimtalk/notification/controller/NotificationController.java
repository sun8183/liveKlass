package com.liveklass.alimtalk.notification.controller;

import com.liveklass.alimtalk.global.response.ApiResponse;
import com.liveklass.alimtalk.global.response.enums.SuccessStatus;
import com.liveklass.alimtalk.notification.dto.request.NotificationRegisterRequest;
import com.liveklass.alimtalk.notification.dto.response.NotificationDetailResponse;
import com.liveklass.alimtalk.notification.dto.response.NotificationRegisterResponse;
import com.liveklass.alimtalk.notification.dto.response.NotificationSummaryResponse;
import com.liveklass.alimtalk.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationRegisterResponse>> register(@Valid @RequestBody NotificationRegisterRequest request) {
        return ApiResponse.of(SuccessStatus.NOTIFICATION_REGISTERED, notificationService.register(request));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationDetailResponse>> getNotification(@PathVariable Long notificationId) {
        return ApiResponse.of(SuccessStatus.NOTIFICATION_RETRIEVED, notificationService.getNotification(notificationId));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationSummaryResponse>>> getNotifications(
            @RequestParam String recipientId,
            @RequestParam(required = false) Boolean read,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.of(SuccessStatus.NOTIFICATION_LIST_RETRIEVED,
                notificationService.getNotifications(recipientId, read, pageable));
    }

    @GetMapping("/failed")
    public ResponseEntity<ApiResponse<Page<NotificationSummaryResponse>>> getFailedNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.of(SuccessStatus.NOTIFICATION_FAILED_LIST_RETRIEVED,
                notificationService.getFailedNotifications(pageable));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long notificationId) {
        notificationService.markRead(notificationId);
        return ApiResponse.of(SuccessStatus.NOTIFICATION_READ);
    }

    @PostMapping("/{notificationId}/retry")
    public ResponseEntity<ApiResponse<Void>> retryManually(@PathVariable Long notificationId) {
        notificationService.retryManually(notificationId);
        return ApiResponse.of(SuccessStatus.NOTIFICATION_RETRIED);
    }
}