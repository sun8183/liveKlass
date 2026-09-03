package com.liveklass.alimtalk.template.service;

import com.liveklass.alimtalk.global.exception.BusinessException;
import com.liveklass.alimtalk.global.response.enums.ErrorStatus;
import com.liveklass.alimtalk.notification.domain.entity.Notification;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.template.domain.entity.Template;
import com.liveklass.alimtalk.template.dto.request.TemplateRequest;
import com.liveklass.alimtalk.template.dto.response.TemplateResponse;
import com.liveklass.alimtalk.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateWriter templateWriter;
    private final ObjectMapper objectMapper;

    /**
     * 있으면 수정, 없으면 생성. 같은 타입으로 첫 등록이 동시에 두 번 들어오면 둘 다 "없음"으로 보고
     * insert를 시도할 수 있는데, 그중 하나는 unique 제약에 걸린다 — 그 경우 update 경로로 전환해서
     * 예외를 던지는 대신 정상적으로 upsert가 수렴하게 한다.
     * create/update는 각각 별도 트랜잭션(TemplateWriter, REQUIRES_NEW)으로 실행한다 — 실패한 insert와
     * 뒤이은 update가 같은 영속성 컨텍스트를 공유하면 Hibernate 세션이 깨진 채로 재사용돼 오류가 난다.
     */
    public TemplateResponse upsert(NotificationType notificationType, TemplateRequest request) {
        boolean exists = templateRepository.findByNotificationType(notificationType).isPresent();
        if (exists) {
            return TemplateResponse.from(templateWriter.update(notificationType, request));
        }

        try {
            return TemplateResponse.from(templateWriter.create(notificationType, request));
        } catch (DataIntegrityViolationException e) {
            return TemplateResponse.from(templateWriter.update(notificationType, request));
        }
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(NotificationType notificationType) {
        return TemplateResponse.from(getEntity(notificationType));
    }

    /**
     * 타입에 등록된 템플릿이 없으면 타입 설명을 기본 메시지로 써서, 템플릿 미등록이 발송 자체를 막지 않게 한다.
     * "{{key}}" 자리표시자는 recipientId와 referenceData의 키로 치환한다.
     */
    @Transactional(readOnly = true)
    public String render(Notification notification) {
        Optional<Template> template = templateRepository.findByNotificationType(notification.getNotificationType());
        if (template.isEmpty()) {
            return notification.getNotificationType().getDescription();
        }

        Map<String, Object> variables = parseReferenceData(notification.getReferenceData());
        variables.put("recipientId", notification.getRecipientId());

        String title = applyPlaceholders(template.get().getTitle(), variables);
        String content = applyPlaceholders(template.get().getContent(), variables);
        return "[" + title + "] " + content;
    }

    private Template getEntity(NotificationType notificationType) {
        return templateRepository.findByNotificationType(notificationType)
                .orElseThrow(() -> new BusinessException(ErrorStatus.NOTIFICATION_TEMPLATE_NOT_FOUND));
    }

    private String applyPlaceholders(String text, Map<String, Object> variables) {
        String result = text;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseReferenceData(String referenceData) {
        if (referenceData == null || referenceData.isBlank()) {
            return new HashMap<>();
        }
        try {
            return new HashMap<>(objectMapper.readValue(referenceData, Map.class));
        } catch (JacksonException e) {
            return new HashMap<>();
        }
    }
}
