package com.errorpurifier.domain.feedback.dto;

import java.util.List;

/** Admin-only, aggregate-only report. No raw console content is returned. */
public record RefinementQualitySummaryResponse(
        FeedbackBreakdown overall,
        FeedbackBreakdown truncatedLogs,
        FeedbackBreakdown nonTruncatedLogs,
        List<CategoryQuality> mostProblematicCategories
) {
    public record FeedbackBreakdown(long total, long appropriate, long missingContext, long tooNoisy) {
    }

    public record CategoryQuality(String category, long feedbackCount, long appliedLines,
                                  long appropriate, long missingContext, long tooNoisy) {
        public long negativeFeedback() {
            return missingContext + tooNoisy;
        }
    }
}
