package com.errorpurifier.benchmark;

import com.errorpurifier.domain.cache.service.LogPromptRefiner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LogRefinementBenchmarkSupportTest {

    @ParameterizedTest
    @ValueSource(ints = {1_000, 10_000, 100_000, 1_000_000})
    void generatedLogsExerciseMaskingCompressionRulesAndPromptLimit(int targetCharacters) {
        String rawLog = LogRefinementBenchmarkSupport.generateLog(targetCharacters);
        LogPromptRefiner.RefinedLog result = LogRefinementBenchmarkSupport.newRefiner()
                .refine(rawLog, null, LogRefinementBenchmarkSupport.PROJECT_TAGS);

        assertThat(rawLog)
                .hasSizeGreaterThanOrEqualTo(targetCharacters)
                .hasSizeLessThan(targetCharacters + 1_024)
                .contains("password=secret-")
                .contains("Authorization: Bearer header.payload.signature")
                .contains("at org.springframework.")
                .contains("at com.errorpurifier.");
        assertThat(result.text())
                .doesNotContain("password=secret-")
                .doesNotContain("Bearer header.payload.signature")
                .contains("password=[REDACTED]")
                .contains("Authorization: Bearer [REDACTED]")
                .contains("at com.errorpurifier.")
                .doesNotContain("at org.springframework.")
                .hasSizeLessThanOrEqualTo(12_000);
        assertThat(result.repeatedBlockCount()).isGreaterThanOrEqualTo(4);
        assertThat(result.omittedRepeatBlockCount()).isPositive();
        assertThat(result.repeatCompressionCharacters()).isPositive();
        assertThat(result.appliedRuleCounts()).containsKey("SPRING");
        assertThat(result.protectedLineCount()).isPositive();
        assertThat(result.truncated()).isEqualTo(targetCharacters >= 100_000);
    }
}
