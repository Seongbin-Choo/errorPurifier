package com.errorpurifier.domain.cache.service;

import com.errorpurifier.domain.rule.entity.LogParsingRule;
import com.errorpurifier.domain.rule.repository.LogParsingRuleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogPromptRefinerTest {

    @Test
    void whitelistProtectsAnExceptionLineFromBlacklistRules() {
        LogParsingRuleRepository repository = mock(LogParsingRuleRepository.class);
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(List.of(
                rule(LogParsingRule.RuleType.WHITELIST, "^[\\w.$]+Exception:.*$"),
                rule(LogParsingRule.RuleType.BLACKLIST, ".*IllegalStateException.*")
        ));
        LogPromptRefiner refiner = new LogPromptRefiner(repository, new SensitiveDataSanitizer(), new RepeatedLogCompressor());

        LogPromptRefiner.RefinedLog result = refiner.refine("java.lang.IllegalStateException: root cause", null);

        assertThat(result.text()).contains("java.lang.IllegalStateException: root cause");
        assertThat(result.protectedLineCount()).isEqualTo(1);
    }

    @Test
    void projectPackageFrameIsProtectedEvenWhenAConflictingBlacklistExists() {
        LogParsingRuleRepository repository = mock(LogParsingRuleRepository.class);
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(List.of(
                rule(LogParsingRule.RuleType.BLACKLIST, "^\\s*at com\\.example\\..*$")
        ));
        LogPromptRefiner refiner = new LogPromptRefiner(repository, new SensitiveDataSanitizer(), new RepeatedLogCompressor());

        LogPromptRefiner.RefinedLog result = refiner.refine(
                "java.lang.IllegalStateException: root cause\n\tat com.example.orders.OrderService.save(OrderService.java:42)",
                null, Map.of("project-package-prefix", "com.example"));

        assertThat(result.text()).contains("OrderService.save(OrderService.java:42)");
        assertThat(result.protectedLineCount()).isEqualTo(1);
    }

    @Test
    void reportsRepeatedBlockCompressionBeforeApplyingLineRules() {
        LogParsingRuleRepository repository = mock(LogParsingRuleRepository.class);
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(List.of());
        LogPromptRefiner refiner = new LogPromptRefiner(repository, new SensitiveDataSanitizer(), new RepeatedLogCompressor());
        StringBuilder log = new StringBuilder();
        for (int attempt = 1; attempt <= 4; attempt++) {
            log.append("2026-08-10 14:02:0").append(attempt)
                    .append(".221 [retry-").append(attempt).append("] WARN reconnect attempt ").append(attempt).append("/4\n")
                    .append("java.net.ConnectException: Connection refused\n");
        }

        LogPromptRefiner.RefinedLog result = refiner.refine(log.toString(), null);

        assertThat(result.repeatedBlockCount()).isEqualTo(4);
        assertThat(result.omittedRepeatBlockCount()).isEqualTo(1);
        assertThat(result.repeatCompressionCharacters()).isPositive();
        assertThat(result.text()).contains("반복 로그 블록 4회 중 1회 생략");
    }

    @Test
    void marksIdeExecutionTrailerAsNonDiagnosticMetadata() {
        LogParsingRuleRepository repository = mock(LogParsingRuleRepository.class);
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(List.of());
        LogPromptRefiner refiner = new LogPromptRefiner(repository, new SensitiveDataSanitizer(), new RepeatedLogCompressor());

        LogPromptRefiner.RefinedLog result = refiner.refine("200 OK\nProcess finished with exit code 1", null);

        assertThat(result.text()).contains("[실행 환경 메타데이터 - 이 로그만으로 종료 원인 판정 불가")
                .contains("Process finished with exit code 1");
    }

    @Test
    void returnsStableGuidanceCodesForKnownUnreadyConditions() {
        LogParsingRuleRepository repository = mock(LogParsingRuleRepository.class);
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(List.of());
        LogPromptRefiner refiner = new LogPromptRefiner(repository, new SensitiveDataSanitizer(), new RepeatedLogCompressor());

        LogPromptRefiner.RefinedLog wrapperOnly = refiner.refine(
                "Execution failed for task ':app.run()'.\nProcess finished with non-zero exit value 1\nBUILD FAILED", null);
        LogPromptRefiner.RefinedLog empty = refiner.refine("   \n", null);

        assertThat(wrapperOnly.readiness().ready()).isFalse();
        assertThat(wrapperOnly.readiness().guidanceCode()).isEqualTo(LogPromptRefiner.GUIDANCE_BUILD_WRAPPER_ONLY);
        assertThat(wrapperOnly.readiness().guidance()).contains("--stacktrace");
        assertThat(empty.readiness().ready()).isFalse();
        assertThat(empty.readiness().guidanceCode()).isEqualTo(LogPromptRefiner.GUIDANCE_NO_ACTIONABLE_LOG);
        assertThat(empty.readiness().guidance()).contains("분석할 로그");
    }

    @Test
    void longSpringDebugLogKeepsDeepestCauseInsteadOfClassNamesContainingError() {
        LogParsingRuleRepository repository = mock(LogParsingRuleRepository.class);
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(List.of());
        LogPromptRefiner refiner = new LogPromptRefiner(repository, new SensitiveDataSanitizer(), new RepeatedLogCompressor());
        StringBuilder log = new StringBuilder();
        appendDistinctDebugNoise(log, 48_000, "scanned InvalidErrorException.class and ErrorMvcAutoConfiguration");
        log.append("Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean 'dataSource'\n");
        appendDistinctDebugNoise(log, 72_000, "condition evaluation after top-level failure");
        log.append("Caused by: org.springframework.boot.autoconfigure.jdbc.DataSourceProperties$DataSourceBeanCreationException: Failed to determine a suitable driver class\n")
                .append("\tat org.springframework.boot.autoconfigure.jdbc.DataSourceProperties.determineDriverClassName(DataSourceProperties.java:252)\n");
        appendDistinctDebugNoise(log, 125_000, "shutdown diagnostics after root cause");

        LogPromptRefiner.RefinedLog result = refiner.refine(log.toString(), null);

        assertThat(result.truncated()).isTrue();
        assertThat(result.text())
                .hasSizeLessThanOrEqualTo(12_000)
                .contains("DataSourceBeanCreationException: Failed to determine a suitable driver class");
    }

    @Test
    void springErrorLineWithSpaceSeparatedDateAndTimeIsUsedAsFallbackAnchor() {
        LogParsingRuleRepository repository = mock(LogParsingRuleRepository.class);
        when(repository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(List.of());
        LogPromptRefiner refiner = new LogPromptRefiner(repository, new SensitiveDataSanitizer(), new RepeatedLogCompressor());
        StringBuilder log = new StringBuilder();
        appendDistinctDebugNoise(log, 20_000, "scanned InvalidErrorException.class and ErrorMvcAutoConfiguration");
        log.append("2026-09-08 09:03:22 ERROR Failed to configure datasource because url is missing\n");
        appendDistinctDebugNoise(log, 45_000, "shutdown details without another failure signal");

        LogPromptRefiner.RefinedLog result = refiner.refine(log.toString(), null);

        assertThat(result.truncated()).isTrue();
        assertThat(result.text())
                .hasSizeLessThanOrEqualTo(12_000)
                .contains("2026-09-08 09:03:22 ERROR Failed to configure datasource because url is missing");
    }

    private void appendDistinctDebugNoise(StringBuilder log, int targetLength, String message) {
        for (int line = 0; log.length() < targetLength; line++) {
            log.append("DEBUG ").append(message).append(" id")
                    .append(alphabeticIdentifier(line)).append('\n');
        }
    }

    private String alphabeticIdentifier(int value) {
        StringBuilder identifier = new StringBuilder();
        do {
            identifier.append((char) ('a' + value % 26));
            value /= 26;
        } while (value > 0);
        return identifier.reverse().toString();
    }

    private LogParsingRule rule(LogParsingRule.RuleType type, String regex) {
        return LogParsingRule.builder()
                .ruleType(type)
                .targetFramework("TEST")
                .regexPattern(regex)
                .priority(100)
                .description("test-" + type + regex)
                .minPluginVersion("1.0.0")
                .build();
    }
}
