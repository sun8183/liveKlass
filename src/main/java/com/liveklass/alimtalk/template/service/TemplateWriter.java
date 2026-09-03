package com.liveklass.alimtalk.template.service;

import com.liveklass.alimtalk.global.exception.BusinessException;
import com.liveklass.alimtalk.global.response.enums.ErrorStatus;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.template.domain.entity.Template;
import com.liveklass.alimtalk.template.dto.request.TemplateRequest;
import com.liveklass.alimtalk.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * create/update를 각각 별도 트랜잭션(REQUIRES_NEW)으로 실행한다.
 * insert가 유니크 제약 위반으로 실패하면 그 트랜잭션(과 영속성 컨텍스트)은 통째로 폐기되고,
 * 뒤이은 update는 완전히 새 영속성 컨텍스트에서 실행된다.
 * 같은 트랜잭션 안에서 실패한 insert 뒤에 바로 update를 이어가면, Hibernate가
 * 이미 깨진 영속성 컨텍스트를 다시 flush하려다 "null identifier" AssertionFailure를 던진다
 * (Hibernate는 flush 예외 발생 후의 세션 재사용을 보장하지 않는다).
 */
@Service
@RequiredArgsConstructor
public class TemplateWriter {

    private final TemplateRepository templateRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Template create(NotificationType notificationType, TemplateRequest request) {
        return templateRepository.save(Template.create(notificationType, request.title(), request.content()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Template update(NotificationType notificationType, TemplateRequest request) {
        Template template = templateRepository.findByNotificationType(notificationType)
                .orElseThrow(() -> new BusinessException(ErrorStatus.NOTIFICATION_TEMPLATE_NOT_FOUND));
        template.update(request.title(), request.content());
        return template;
    }
}
