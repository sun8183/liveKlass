package com.liveklass.alimtalk.template.controller;

import com.liveklass.alimtalk.global.exception.BusinessException;
import com.liveklass.alimtalk.global.response.enums.ErrorStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.template.dto.response.TemplateResponse;
import com.liveklass.alimtalk.template.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TemplateController.class)
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TemplateService templateService;

    @Value("${admin.api-key}")
    private String adminApiKey;

    @Test
    void 운영자키가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(put("/api/notification-templates/ENROLLMENT_COMPLETED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E004"));
    }

    @Test
    void 운영자키가_틀리면_401을_반환한다() throws Exception {
        mockMvc.perform(put("/api/notification-templates/ENROLLMENT_COMPLETED")
                        .header("X-Admin-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E004"));
    }

    @Test
    void 운영자키가_올바르면_200을_반환한다() throws Exception {
        given(templateService.upsert(any(), any()))
                .willReturn(new TemplateResponse(NotificationType.ENROLLMENT_COMPLETED, "t", "c"));

        mockMvc.perform(put("/api/notification-templates/ENROLLMENT_COMPLETED")
                        .header("X-Admin-Key", adminApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"content\":\"c\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("S007"));
    }

    @Test
    void 템플릿이_없으면_404를_반환한다() throws Exception {
        given(templateService.getTemplate(NotificationType.LECTURE_STARTING_SOON))
                .willThrow(new BusinessException(ErrorStatus.NOTIFICATION_TEMPLATE_NOT_FOUND));

        mockMvc.perform(get("/api/notification-templates/LECTURE_STARTING_SOON")
                        .header("X-Admin-Key", adminApiKey))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("N004"));
    }
}
