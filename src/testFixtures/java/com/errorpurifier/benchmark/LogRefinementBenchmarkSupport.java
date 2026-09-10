package com.errorpurifier.benchmark;

import com.errorpurifier.domain.cache.service.LogPromptRefiner;
import com.errorpurifier.domain.cache.service.RepeatedLogCompressor;
import com.errorpurifier.domain.cache.service.SensitiveDataSanitizer;
import com.errorpurifier.domain.rule.entity.LogParsingRule;
import com.errorpurifier.domain.rule.repository.LogParsingRuleRepository;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LogRefinementBenchmarkSupport {

    public static final int[] INPUT_SIZES = {1_000, 10_000, 100_000, 1_000_000};
    public static final Map<String, String> PROJECT_TAGS = Map.of(
            "project-package-prefix", "com.errorpurifier"
    );

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 9, 8, 9, 3, 22);

    private LogRefinementBenchmarkSupport() {
    }

    public static LogPromptRefiner newRefiner() {
        List<LogParsingRule> rules = List.of(
                rule(LogParsingRule.RuleType.WHITELIST, "APPLICATION",
                        "^\\s*at com\\.errorpurifier\\..*$", 300),
                rule(LogParsingRule.RuleType.BLACKLIST, "SPRING",
                        "^\\s*at org\\.springframework\\..*$", 200),
                rule(LogParsingRule.RuleType.BLACKLIST, "HIKARI",
                        "^.*HikariPool.*housekeeper.*$", 100)
        );
        LogParsingRuleRepository repository = (LogParsingRuleRepository) Proxy.newProxyInstance(
                LogParsingRuleRepository.class.getClassLoader(),
                new Class<?>[]{LogParsingRuleRepository.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "findByIsActiveTrueOrderByPriorityDesc" -> rules;
                    case "toString" -> "BenchmarkLogParsingRuleRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new UnsupportedOperationException(
                            "Benchmark repository does not implement " + method.getName());
                }
        );
        return new LogPromptRefiner(repository, new SensitiveDataSanitizer(), new RepeatedLogCompressor());
    }

    public static String generateLog(int targetCharacters) {
        if (targetCharacters < 1_000) {
            throw new IllegalArgumentException("targetCharacters must be at least 1,000");
        }

        StringBuilder log = new StringBuilder(targetCharacters + 512);
        int repeatedRegionTarget = Math.max(1_000, targetCharacters / 2);
        int retry = 1;
        while (retry <= 4 || log.length() < repeatedRegionTarget) {
            appendBlock(log, retry, "database connection retry", false);
            retry++;
        }
        while (log.length() < targetCharacters) {
            appendBlock(log, retry, "diagnostic phase " + alphabeticIdentifier(retry), true);
            retry++;
        }
        return log.toString();
    }

    private static void appendBlock(StringBuilder log, int retry, String detail, boolean includePoolNoise) {
        if (!log.isEmpty()) {
            log.append('\n');
        }
        String uuid = UUID.nameUUIDFromBytes(("retry-" + retry).getBytes(StandardCharsets.UTF_8)).toString();
        String timestamp = BASE_TIME.plusSeconds(retry).plusNanos((retry % 1_000) * 1_000_000L)
                .format(TIMESTAMP_FORMAT);
        log.append(timestamp)
                .append(" ERROR retry=").append(retry)
                .append(" requestId=").append(uuid)
                .append(" password=secret-").append(retry)
                .append(" Authorization: Bearer header.payload.signature").append(retry).append('\n')
                .append("java.lang.IllegalStateException: ").append(detail).append(" after ").append(retry).append("ms\n")
                .append("\tat org.springframework.jdbc.core.JdbcTemplate.execute(JdbcTemplate.java:")
                .append(100 + retry % 50).append(")\n")
                .append("\tat com.errorpurifier.order.OrderService.retry(OrderService.java:")
                .append(40 + retry % 20).append(")");
        if (includePoolNoise) {
            log.append('\n').append("HikariPool-").append(retry)
                    .append(" housekeeper connection check ").append(alphabeticIdentifier(retry));
        }
    }

    private static String alphabeticIdentifier(int value) {
        StringBuilder identifier = new StringBuilder();
        do {
            identifier.append((char) ('a' + value % 26));
            value /= 26;
        } while (value > 0);
        return identifier.reverse().toString();
    }

    private static LogParsingRule rule(LogParsingRule.RuleType type, String framework, String regex, int priority) {
        return LogParsingRule.builder()
                .ruleType(type)
                .targetFramework(framework)
                .regexPattern(regex)
                .priority(priority)
                .description("JMH " + framework + " rule")
                .minPluginVersion("1.0.0")
                .build();
    }
}
