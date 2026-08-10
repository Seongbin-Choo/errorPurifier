package com.errorpurifier.domain.client.dto;

import java.util.List;
import java.util.UUID;

public record InitResponse(
        String deviceUuid,
        List<RuleDto> parsingRules
) {
    public record RuleDto(
            String ruleType,
            String targetFramework,
            String regexPattern,
            int priority
    ) {}
}