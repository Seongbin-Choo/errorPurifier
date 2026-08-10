package com.errorpurifier.domain.usage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record LlmFeedbackRequest(
        @Min(-1) @Max(1) int rating,
        boolean resolved
) {
}
