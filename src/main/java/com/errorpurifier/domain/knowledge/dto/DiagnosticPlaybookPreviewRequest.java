package com.errorpurifier.domain.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosticPlaybookPreviewRequest(
        @NotBlank @Size(max = 30_000) String log
) {
}
