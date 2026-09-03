package com.liveklass.alimtalk.template.service;

import com.liveklass.alimtalk.global.exception.BusinessException;
import com.liveklass.alimtalk.global.response.enums.ErrorStatus;
import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationChannel;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.template.domain.entity.Template;
import com.liveklass.alimtalk.template.dto.request.TemplateRequest;
import com.liveklass.alimtalk.template.dto.response.TemplateResponse;
import com.liveklass.alimtalk.template.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateWriter templateWriter;

    private TemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new TemplateService(templateRepository, templateWriter, new JsonMapper());
    }

    @Test
    void 템플릿이_없으면_타입설명을_반환한다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);
        given(templateRepository.findByNotificationType(NotificationType.ENROLLMENT_COMPLETED))
                .willReturn(Optional.empty());

        String message = templateService.render(notification);

        assertThat(message).isEqualTo(NotificationType.ENROLLMENT_COMPLETED.getDescription());
    }

    @Test
    void 템플릿이_있으면_메시지를_치환한다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1",
                "{\"courseName\":\"스프링 기초\"}", NotificationChannel.EMAIL, null);
        Template template = Template.create(NotificationType.ENROLLMENT_COMPLETED,
                "수강신청 완료", "{{recipientId}}님, {{courseName}} 신청이 완료되었습니다.");
        given(templateRepository.findByNotificationType(NotificationType.ENROLLMENT_COMPLETED))
                .willReturn(Optional.of(template));

        String message = templateService.render(notification);

        assertThat(message).isEqualTo("[수강신청 완료] user-1님, 스프링 기초 신청이 완료되었습니다.");
    }

    @Test
    void 치환할_값이_없으면_메시지내용을_유지한다() {
        Notification notification = Notification.createPending(
                "user-1", NotificationType.ENROLLMENT_COMPLETED, "evt-1", null, NotificationChannel.EMAIL, null);
        Template template = Template.create(NotificationType.ENROLLMENT_COMPLETED,
                "제목", "{{recipientId}}님, {{courseName}} 안내");
        given(templateRepository.findByNotificationType(NotificationType.ENROLLMENT_COMPLETED))
                .willReturn(Optional.of(template));

        String message = templateService.render(notification);

        assertThat(message).isEqualTo("[제목] user-1님, {{courseName}} 안내");
    }

    @Test
    void 템플릿이_없으면_조회시_예외가_발생한다() {
        given(templateRepository.findByNotificationType(NotificationType.PAYMENT_CONFIRMED))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.getTemplate(NotificationType.PAYMENT_CONFIRMED))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(ErrorStatus.NOTIFICATION_TEMPLATE_NOT_FOUND);
    }

    @Test
    void 템플릿이_없으면_생성을_호출한다() {
        TemplateRequest request = new TemplateRequest("title", "content");
        given(templateRepository.findByNotificationType(NotificationType.PAYMENT_CONFIRMED))
                .willReturn(Optional.empty());
        given(templateWriter.create(NotificationType.PAYMENT_CONFIRMED, request))
                .willReturn(Template.create(NotificationType.PAYMENT_CONFIRMED, "title", "content"));

        templateService.upsert(NotificationType.PAYMENT_CONFIRMED, request);

        verify(templateWriter).create(NotificationType.PAYMENT_CONFIRMED, request);
        verify(templateWriter, never()).update(any(), any());
    }

    @Test
    void 템플릿이_있으면_수정을_호출한다() {
        TemplateRequest request = new TemplateRequest("title", "content");
        given(templateRepository.findByNotificationType(NotificationType.PAYMENT_CONFIRMED))
                .willReturn(Optional.of(Template.create(NotificationType.PAYMENT_CONFIRMED, "old", "old")));
        given(templateWriter.update(NotificationType.PAYMENT_CONFIRMED, request))
                .willReturn(Template.create(NotificationType.PAYMENT_CONFIRMED, "title", "content"));

        templateService.upsert(NotificationType.PAYMENT_CONFIRMED, request);

        verify(templateWriter).update(NotificationType.PAYMENT_CONFIRMED, request);
        verify(templateWriter, never()).create(any(), any());
    }

    @Test
    void 생성이_충돌하면_수정으로_전환한다() {
        TemplateRequest request = new TemplateRequest("title", "content");
        given(templateRepository.findByNotificationType(NotificationType.PAYMENT_CONFIRMED))
                .willReturn(Optional.empty());
        given(templateWriter.create(NotificationType.PAYMENT_CONFIRMED, request))
                .willThrow(new DataIntegrityViolationException("race"));
        given(templateWriter.update(NotificationType.PAYMENT_CONFIRMED, request))
                .willReturn(Template.create(NotificationType.PAYMENT_CONFIRMED, "title", "content"));

        TemplateResponse response = templateService.upsert(NotificationType.PAYMENT_CONFIRMED, request);

        assertThat(response.title()).isEqualTo("title");
        verify(templateWriter).update(NotificationType.PAYMENT_CONFIRMED, request);
    }
}
