package com.errorpurifier.domain.rule.dto;

import com.errorpurifier.domain.rule.entity.LogParsingRule.RuleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LogParsingRuleRequest(
        @NotNull RuleType ruleType,
        @NotBlank @Size(max = 50) String targetFramework,
        @NotBlank @Size(max = 500) String regexPattern,
        @Min(0) @Max(10_000) int priority,
        @NotBlank @Size(max = 255) String description,
        @NotBlank @Pattern(regexp = "\\d+\\.\\d+\\.\\d+", message = "최소 플러그인 버전은 x.y.z 형식이어야 합니다.") String minPluginVersion
) {
}
