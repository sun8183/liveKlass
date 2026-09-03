package com.liveklass.alimtalk.template.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TemplateRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
