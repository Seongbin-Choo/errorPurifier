package com.errorpurifier.domain.rule.dto;

import com.errorpurifier.domain.rule.entity.LogParsingRule;

public record LogParsingRuleResponse(Long id, String ruleType, String targetFramework, String regexPattern,
                                     int priority, String description, String minPluginVersion, boolean active) {
    public static LogParsingRuleResponse from(LogParsingRule rule) {
        return new LogParsingRuleResponse(rule.getId(), rule.getRuleType().name(), rule.getTargetFramework(),
                rule.getRegexPattern(), rule.getPriority(), rule.getDescription(), rule.getMinPluginVersion(),
                rule.isActive());
    }
}
