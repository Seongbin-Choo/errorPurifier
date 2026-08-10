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
        LogPromptRefiner refiner = new LogPromptRefiner(repository, new SensitiveDataSanitizer());

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
        LogPromptRefiner refiner = new LogPromptRefiner(repository, new SensitiveDataSanitizer());

        LogPromptRefiner.RefinedLog result = refiner.refine(
                "java.lang.IllegalStateException: root cause\n\tat com.example.orders.OrderService.save(OrderService.java:42)",
                null, Map.of("project-package-prefix", "com.example"));

        assertThat(result.text()).contains("OrderService.save(OrderService.java:42)");
        assertThat(result.protectedLineCount()).isEqualTo(1);
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
