package com.errorpurifier.domain.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosticPlaybookRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 1000) String matchPattern,
        @NotBlank @Size(max = 5000) String guidance,
        @Min(0) @Max(10_000) int priority
) {
}
