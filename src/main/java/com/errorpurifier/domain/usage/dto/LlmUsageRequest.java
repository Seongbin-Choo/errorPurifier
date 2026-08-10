package com.errorpurifier.domain.usage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LlmUsageRequest(
        @NotBlank @Size(max = 20) String provider,
        @NotBlank @Size(max = 100) String model,
        @NotBlank @Size(min = 64, max = 64) String cacheKey,
        boolean cacheHit,
        @NotBlank @Size(min = 64, max = 64) String promptHash,
        @PositiveOrZero int originalCharacters,
        @PositiveOrZero int preparedCharacters,
        @PositiveOrZero int repeatCompressionCharacters,
        @PositiveOrZero int inputTokens,
        @PositiveOrZero int outputTokens,
        @PositiveOrZero int thinkingTokens,
        @PositiveOrZero int totalTokens,
        @PositiveOrZero long latencyMs,
        @Size(max = 100) List<@Size(max = 10) String> referencedLines,
        @Min(-1) @Max(1) int rating
) {
}
