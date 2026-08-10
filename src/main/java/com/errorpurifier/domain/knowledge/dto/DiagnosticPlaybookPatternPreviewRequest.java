package com.errorpurifier.domain.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosticPlaybookPatternPreviewRequest(
        @NotBlank @Size(max = 1_000) String matchPattern,
        @NotBlank @Size(max = 30_000) String log
) {
}
