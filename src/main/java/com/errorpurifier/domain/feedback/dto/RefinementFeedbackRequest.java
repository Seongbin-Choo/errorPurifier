package com.errorpurifier.domain.feedback.dto;

import com.errorpurifier.domain.feedback.entity.RefinementFeedbackType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record RefinementFeedbackRequest(
        @NotBlank @Size(min = 64, max = 64) String cacheKey,
        @NotNull RefinementFeedbackType feedbackType,
        @PositiveOrZero int originalCharacters,
        @PositiveOrZero int preparedCharacters,
        @NotNull @Size(max = 30) Map<@Size(max = 30) String, @PositiveOrZero Integer> appliedRuleCounts,
        @PositiveOrZero int protectedLineCount,
        boolean logTruncated
) {
}
