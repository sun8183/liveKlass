package com.liveklass.alimtalk.template.domain.entity;

import com.liveklass.alimtalk.global.entity.BaseEntity;
import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 타입별 메시지 템플릿. 타입당 1개만 존재(unique 제약)하고, upsert로 등록/수정한다.
 * content에는 "{{key}}" 형태의 자리표시자를 쓸 수 있고, 발송 시 recipientId + referenceData의 키로 치환한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notification_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_template_notification_type", columnNames = "notification_type")
)
public class Template extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50, unique = true)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    private Template(NotificationType notificationType, String title, String content) {
        this.notificationType = notificationType;
        this.title = title;
        this.content = content;
    }

    public static Template create(NotificationType notificationType, String title, String content) {
        return new Template(notificationType, title, content);
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
