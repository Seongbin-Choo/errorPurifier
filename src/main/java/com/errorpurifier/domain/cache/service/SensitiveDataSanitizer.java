package com.errorpurifier.domain.cache.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Removes common credentials before a log is fingerprinted, cached, or sent to a third-party LLM.
 * It deliberately keeps the surrounding diagnostic text so the error remains understandable.
 */
@Component
public class SensitiveDataSanitizer {

    private static final String REDACTED = "[REDACTED]";

    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile("(?s)-----BEGIN (?:[A-Z ]+ )?PRIVATE KEY-----.*?-----END (?:[A-Z ]+ )?PRIVATE KEY-----"), REDACTED),
            new Rule(Pattern.compile("(?i)(authorization\\s*:\\s*(?:bearer|basic)\\s+)[^\\s]+"), "$1" + REDACTED),
            new Rule(Pattern.compile("(?i)(\\bbearer\\s+)[A-Za-z0-9._~+\\-/]+=*"), "$1" + REDACTED),
            new Rule(Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"), REDACTED),
            new Rule(Pattern.compile("\\bAIza[0-9A-Za-z_\\-]{20,}\\b"), REDACTED),
            new Rule(Pattern.compile("\\bsk-(?:proj-)?[A-Za-z0-9_\\-]{20,}\\b"), REDACTED),
            new Rule(Pattern.compile("(?i)(jdbc:[^\\s\\\"']*[?&](?:user|username|password)=)[^&#\\s\\\"']+"), "$1" + REDACTED),
            new Rule(Pattern.compile("(?i)(\\b(?:password|passwd|pwd|api[_-]?key|access[_-]?token|refresh[_-]?token|client[_-]?secret|secret)\\b\\s*[:=]\\s*)([^\\s,;\\\"']+)"), "$1" + REDACTED)
    );

    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String sanitized = input;
        for (Rule rule : RULES) {
            sanitized = rule.pattern().matcher(sanitized).replaceAll(rule.replacement());
        }
        return sanitized;
    }

    private record Rule(Pattern pattern, String replacement) {
    }
}
