package com.liveklass.alimtalk.template.repository;

import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.template.domain.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    Optional<Template> findByNotificationType(NotificationType notificationType);
}
