package com.liveklass.alimtalk.template.service;

import com.liveklass.alimtalk.notification.domain.enums.NotificationType;
import com.liveklass.alimtalk.support.ConcurrencyTestHelper;
import com.liveklass.alimtalk.template.dto.request.TemplateRequest;
import com.liveklass.alimtalk.template.repository.TemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TemplateServiceConcurrencyTest {

    private static final NotificationType TYPE = NotificationType.LECTURE_STARTING_SOON;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateRepository templateRepository;

    @BeforeEach
    void cleanUp() {
        templateRepository.findByNotificationType(TYPE).ifPresent(templateRepository::delete);
    }

    @Test
    void 같은_타입을_동시에_upsert해도_하나의_행으로_수렴한다() throws InterruptedException {
        int threadCount = 10;
        List<Throwable> unexpected = new CopyOnWriteArrayList<>();

        ConcurrencyTestHelper.runConcurrently(threadCount, () -> {
            try {
                templateService.upsert(TYPE, new TemplateRequest("강의 시작 임박", "{{recipientId}}님, 곧 강의가 시작됩니다."));
            } catch (Exception e) {
                unexpected.add(e);
            }
        });

        assertThat(unexpected).isEmpty();
        assertThat(templateRepository.findByNotificationType(TYPE)).isPresent();
    }
}
