package com.errorpurifier.domain.rule.service;

import com.errorpurifier.domain.rule.dto.LogParsingRuleRequest;
import com.errorpurifier.domain.rule.entity.LogParsingRule;
import com.errorpurifier.domain.rule.repository.LogParsingRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogParsingRuleServiceTest {

    @Mock
    private LogParsingRuleRepository ruleRepository;

    @InjectMocks
    private LogParsingRuleService ruleService;

    @Test
    void createsAnActiveRule() {
        LogParsingRuleRequest request = request("custom rule");
        when(ruleRepository.save(any(LogParsingRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = ruleService.create(request);

        assertThat(response.description()).isEqualTo("custom rule");
        assertThat(response.active()).isTrue();
        verify(ruleRepository).save(any(LogParsingRule.class));
    }

    @Test
    void rejectsDuplicatedDescription() {
        when(ruleRepository.existsByDescription("custom rule")).thenReturn(true);

        assertThatThrownBy(() -> ruleService.create(request("custom rule")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
                .isEqualTo(409);
        verify(ruleRepository, never()).save(any());
    }

    @Test
    void deactivatesExistingRule() {
        LogParsingRule rule = rule();
        when(ruleRepository.findById(3L)).thenReturn(java.util.Optional.of(rule));

        ruleService.setActive(3L, false);

        assertThat(rule.isActive()).isFalse();
    }

    private LogParsingRuleRequest request(String description) {
        return new LogParsingRuleRequest(LogParsingRule.RuleType.BLACKLIST, "SPRING", "^at internal.*$", 100,
                description, "1.0.0");
    }

    private LogParsingRule rule() {
        return LogParsingRule.builder()
                .ruleType(LogParsingRule.RuleType.BLACKLIST)
                .targetFramework("SPRING")
                .regexPattern("^at internal.*$")
                .priority(100)
                .description("custom rule")
                .minPluginVersion("1.0.0")
                .build();
    }
}
