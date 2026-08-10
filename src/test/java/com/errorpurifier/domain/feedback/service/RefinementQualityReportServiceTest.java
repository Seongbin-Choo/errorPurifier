package com.errorpurifier.domain.feedback.service;

import com.errorpurifier.domain.feedback.dto.RefinementQualitySummaryResponse;
import com.errorpurifier.domain.feedback.entity.RefinementFeedback;
import com.errorpurifier.domain.feedback.entity.RefinementFeedbackType;
import com.errorpurifier.domain.feedback.repository.RefinementFeedbackRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RefinementQualityReportServiceTest {

    @Test
    void summarizesFeedbackWithoutUsingRawLogs() {
        RefinementFeedbackRepository repository = mock(RefinementFeedbackRepository.class);
        when(repository.findAll()).thenReturn(List.of(
                feedback(RefinementFeedbackType.MISSING_CONTEXT, true, Map.of("SPRING", 8)),
                feedback(RefinementFeedbackType.TOO_NOISY, false, Map.of("GRADLE", 3, "SPRING", 2)),
                feedback(RefinementFeedbackType.APPROPRIATE, false, Map.of("GRADLE", 2))
        ));

        RefinementQualitySummaryResponse summary = new RefinementQualityReportService(repository).summary();

        assertThat(summary.overall()).extracting("total", "appropriate", "missingContext", "tooNoisy")
                .containsExactly(3L, 1L, 1L, 1L);
        assertThat(summary.truncatedLogs().total()).isEqualTo(1);
        assertThat(summary.mostProblematicCategories().getFirst())
                .extracting("category", "feedbackCount", "appliedLines", "missingContext")
                .containsExactly("SPRING", 2L, 10L, 1L);
    }

    private RefinementFeedback feedback(RefinementFeedbackType type, boolean truncated, Map<String, Integer> rules) {
        return RefinementFeedback.builder()
                .feedbackType(type)
                .originalCharacters(100)
                .preparedCharacters(50)
                .appliedRuleCounts(rules)
                .protectedLineCount(1)
                .logTruncated(truncated)
                .build();
    }
}
