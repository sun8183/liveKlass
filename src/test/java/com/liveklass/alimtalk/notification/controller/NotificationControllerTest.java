package com.liveklass.alimtalk.notification.controller;

import com.liveklass.alimtalk.global.exception.BusinessException;
import com.liveklass.alimtalk.global.response.enums.ErrorStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.notification.dto.response.NotificationDetailResponse;
import com.liveklass.alimtalk.notification.dto.response.NotificationRegisterResponse;
import com.liveklass.alimtalk.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Value("${admin.api-key}")
    private String adminApiKey;

    @Test
    void 정상요청이면_202를_반환한다() throws Exception {
        given(notificationService.register(any()))
                .willReturn(new NotificationRegisterResponse(1L, LocalDateTime.now()));

        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "user-1",
                                  "notificationType": "ENROLLMENT_COMPLETED",
                                  "eventId": "evt-1",
                                  "channel": "IN_APP"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.notificationId").value(1));
    }

    @Test
    void 수신자아이디가_비어있으면_400과_필드에러를_반환한다() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "",
                                  "notificationType": "ENROLLMENT_COMPLETED",
                                  "eventId": "evt-1",
                                  "channel": "IN_APP"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E001"))
                .andExpect(jsonPath("$.data[0].field").value("recipientId"));
    }

    @Test
    void 허용되지않는_채널값이면_400과_허용값목록을_반환한다() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "user-1",
                                  "notificationType": "ENROLLMENT_COMPLETED",
                                  "eventId": "evt-1",
                                  "channel": "SMS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E001"))
                .andExpect(jsonPath("$.data[0].field").value("channel"));
    }

    @Test
    void 알림이_존재하면_200을_반환한다() throws Exception {
        NotificationDetailResponse response = new NotificationDetailResponse(
                1L, "user-1", NotificationType.ENROLLMENT_COMPLETED, NotificationChannel.IN_APP,
                NotificationStatus.SUCCESS, "발송 성공", null, 0, false,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        given(notificationService.getNotification(1L)).willReturn(response);

        mockMvc.perform(get("/api/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S002"))
                .andExpect(jsonPath("$.data.notificationId").value(1));
    }

    @Test
    void 알림이_존재하지_않으면_404를_반환한다() throws Exception {
        given(notificationService.getNotification(999L))
                .willThrow(new BusinessException(ErrorStatus.NOTIFICATION_NOT_FOUND));

        mockMvc.perform(get("/api/notifications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("N002"));
    }

    @Test
    void 목록조회는_200과_페이지를_반환한다() throws Exception {
        given(notificationService.getNotifications(eq("user-1"), any(), any()))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/notifications").param("recipientId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S003"));
    }

    @Test
    void 읽음처리는_200을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S004"));
    }

    @Test
    void 재시도가_허용되지_않으면_409를_반환한다() throws Exception {
        doThrow(new BusinessException(ErrorStatus.NOTIFICATION_RETRY_NOT_ALLOWED))
                .when(notificationService).retryManually(1L);

        mockMvc.perform(post("/api/notifications/1/retry").header("X-Admin-Key", adminApiKey))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("N003"));
    }

    @Test
    void 재시도는_운영자키가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/notifications/1/retry"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E004"));
    }

    @Test
    void 최종실패_목록조회는_운영자키가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/notifications/failed"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E004"));
    }

    @Test
    void 최종실패_목록조회는_운영자키가_있으면_200을_반환한다() throws Exception {
        given(notificationService.getFailedNotifications(any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/notifications/failed").header("X-Admin-Key", adminApiKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S006"));
    }
}
