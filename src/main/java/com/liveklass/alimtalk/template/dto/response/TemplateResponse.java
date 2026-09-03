package com.liveklass.alimtalk.template.dto.response;

import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.template.domain.entity.Template;

public record TemplateResponse(
        NotificationType notificationType,
        String title,
        String content
) {
    public static TemplateResponse from(Template template) {
        return new TemplateResponse(
                template.getNotificationType(),
                template.getTitle(),
                template.getContent()
        );
    }
}
