package com.errorpurifier.domain.cache.service;

import com.errorpurifier.domain.rule.entity.LogParsingRule;
import com.errorpurifier.domain.rule.repository.LogParsingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogPromptRefiner {

    public static final String GUIDANCE_BUILD_WRAPPER_ONLY = "BUILD_WRAPPER_ONLY";
    public static final String GUIDANCE_NO_ACTIONABLE_LOG = "NO_ACTIONABLE_LOG";

    private static final int MAX_PROMPT_LOG_CHARACTERS = 12_000;
    private static final Pattern ERROR_ANCHOR = Pattern.compile("(?m)^.*(?:Caused by:|Exception|Error).*$");
    private static final Pattern EXECUTION_TRAILER = Pattern.compile("(?i)^.*(?:process finished with exit code|exit code\\s*\\d+|종료 코드\\s*\\d+).*$");
    private static final String EXECUTION_METADATA_PREFIX = "[실행 환경 메타데이터 - 이 로그만으로 종료 원인 판정 불가, 별도 애플리케이션 종료 로그 필요] ";

    private final LogParsingRuleRepository ruleRepository;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;
    private final RepeatedLogCompressor repeatedLogCompressor;

    public RefinedLog refine(String rawLog, String selectedText) {
        return refine(rawLog, selectedText, Map.of());
    }

    public RefinedLog refine(String rawLog, String selectedText, Map<String, String> projectTags) {
        String source = selectedText != null && !selectedText.isBlank() ? selectedText : rawLog;
        int sourceCharacters = source.length();
        String projectPackagePrefix = projectTags.getOrDefault("project-package-prefix", "");
        RepeatedLogCompressor.CompressionResult compression = compressRepeatedBlocks(source);
        RuleApplication ruleApplication = applyRules(compression.text(), projectPackagePrefix);
        String refined = ruleApplication.text();
        Readiness readiness = assessReadiness(refined);

        if (!readiness.ready() && selectedText != null && !selectedText.isBlank() && rawLog != null && !rawLog.equals(selectedText)) {
            source = rawLog;
            sourceCharacters = source.length();
            compression = compressRepeatedBlocks(rawLog);
            ruleApplication = applyRules(compression.text(), projectPackagePrefix);
            refined = ruleApplication.text();
            readiness = assessReadiness(refined);
        }

        TruncatedLog truncatedLog = trimForPrompt(refined);
        return new RefinedLog(truncatedLog.text(), detectExceptionType(truncatedLog.text()), readiness, sourceCharacters,
                truncatedLog.truncated(), ruleApplication.appliedRuleCounts(), ruleApplication.protectedLineCount(),
                compression.repeatedBlockCount(), compression.omittedBlockCount(), compression.savedCharacters());
    }

    private RepeatedLogCompressor.CompressionResult compressRepeatedBlocks(String source) {
        return repeatedLogCompressor.compress(normalizeWhitespace(sensitiveDataSanitizer.sanitize(source)));
    }

    private RuleApplication applyRules(String text, String projectPackagePrefix) {
        List<CompiledRule> blacklistRules = new ArrayList<>();
        List<Pattern> protectedLinePatterns = new ArrayList<>();
        List<CompiledRule> extractRules = new ArrayList<>();
        for (LogParsingRule rule : ruleRepository.findByIsActiveTrueOrderByPriorityDesc()) {
            try {
                Pattern pattern = Pattern.compile(rule.getRegexPattern(), Pattern.MULTILINE);
                switch (rule.getRuleType()) {
                    case WHITELIST -> protectedLinePatterns.add(pattern);
                    case BLACKLIST -> blacklistRules.add(new CompiledRule(rule, pattern));
                    case EXTRACT -> extractRules.add(new CompiledRule(rule, pattern));
                }
            } catch (PatternSyntaxException exception) {
                log.warn("잘못된 로그 정제 정규식입니다. ruleId={}", rule.getId());
            }
        }
        if (projectPackagePrefix.matches("[A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)+")) {
            protectedLinePatterns.add(Pattern.compile("^\\s*at " + Pattern.quote(projectPackagePrefix) + "(?:\\.|\\$).*"));
        }

        List<String> refinedLines = new ArrayList<>();
        Map<String, Integer> appliedRuleCounts = new LinkedHashMap<>();
        int protectedLineCount = 0;
        for (String line : text.split("\\R", -1)) {
            if (isProtected(line, protectedLinePatterns)) {
                refinedLines.add(line);
                protectedLineCount++;
                continue;
            }
            String filtered = line;
            for (CompiledRule rule : blacklistRules) {
                String next = rule.pattern().matcher(filtered).replaceAll("");
                if (!next.equals(filtered)) {
                    appliedRuleCounts.merge(rule.rule().getTargetFramework(), 1, Integer::sum);
                }
                filtered = next;
            }
            if (!filtered.isBlank()) {
                refinedLines.add(filtered);
            }
        }
        String refined = String.join("\n", refinedLines);
        for (CompiledRule rule : extractRules) {
            refined = extractMatches(refined, rule.pattern());
        }
        return new RuleApplication(normalizeWhitespace(markExecutionMetadata(refined)), Map.copyOf(appliedRuleCounts), protectedLineCount);
    }

    public String normalizeForFingerprint(String value) {
        return value
                .replaceAll("0x[0-9a-fA-F]+", "0x#")
                .replaceAll("(?<![A-Za-z])[0-9]{2,}(?![A-Za-z])", "#")
                .replaceAll("[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}", "<uuid>")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractMatches(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.groupCount() > 0 ? matcher.group(1) : matcher.group());
        }
        return matches.isEmpty() ? text : String.join("\n", matches);
    }

    private boolean isProtected(String line, List<Pattern> protectedLinePatterns) {
        return protectedLinePatterns.stream().anyMatch(pattern -> pattern.matcher(line).find());
    }

    private String markExecutionMetadata(String text) {
        return java.util.Arrays.stream(text.split("\\R", -1))
                .map(line -> EXECUTION_TRAILER.matcher(line).matches() && !line.startsWith(EXECUTION_METADATA_PREFIX)
                        ? EXECUTION_METADATA_PREFIX + line : line)
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String normalizeWhitespace(String text) {
        return text.replace("\r\n", "\n").replaceAll("\n{3,}", "\n\n").trim();
    }

    private String detectExceptionType(String log) {
        Matcher matcher = Pattern.compile("([\\w.$]+(?:Exception|Error))").matcher(log);
        return matcher.find() ? matcher.group(1) : "UNKNOWN";
    }

    private Readiness assessReadiness(String log) {
        boolean hasException = !"UNKNOWN".equals(detectExceptionType(log)) || log.contains("Caused by:");
        boolean isGradleWrapperFailure = log.contains("Execution failed for task")
                || log.contains("non-zero exit value") || log.contains("BUILD FAILED");
        if (isGradleWrapperFailure && !hasException) {
            return new Readiness(false, GUIDANCE_BUILD_WRAPPER_ONLY,
                    "현재 콘솔에는 Gradle 실행 실패 요약만 있고 애플리케이션 예외가 없습니다. "
                            + "Run 콘솔을 위로 올려 첫 `Exception in thread`, `ERROR`, 또는 `Caused by:` 줄부터 아래 스택트레이스까지 선택해 다시 실행하세요. "
                            + "Gradle 자체 설정 오류가 의심되면 실행 명령에 `--stacktrace`를 추가하세요.");
        }
        if (log.isBlank()) {
            return new Readiness(false, GUIDANCE_NO_ACTIONABLE_LOG,
                    "정제 후 분석할 로그가 남아 있지 않습니다. 예외 메시지와 스택트레이스가 포함된 구간을 선택하세요.");
        }
        return new Readiness(true, null, null);
    }

    private TruncatedLog trimForPrompt(String log) {
        if (log.length() <= MAX_PROMPT_LOG_CHARACTERS) {
            return new TruncatedLog(log, false);
        }
        Matcher matcher = ERROR_ANCHOR.matcher(log);
        int anchor = matcher.find() ? matcher.start() : 0;
        int before = 1_500;
        int after = 8_000;
        int start = Math.max(0, anchor - before);
        int end = Math.min(log.length(), anchor + after);
        String focused = log.substring(start, end);
        int remaining = MAX_PROMPT_LOG_CHARACTERS - focused.length();
        if (end < log.length() && remaining > 0) {
            int tailLength = Math.min(remaining, 2_000);
            String marker = "\n\n[... 긴 로그의 중간 구간 생략 ...]\n\n";
            focused = focused + marker + log.substring(log.length() - tailLength);
        }
        if (focused.length() > MAX_PROMPT_LOG_CHARACTERS) {
            focused = focused.substring(0, MAX_PROMPT_LOG_CHARACTERS);
        }
        return new TruncatedLog(focused, true);
    }

    public record RefinedLog(String text, String exceptionType, Readiness readiness, int sourceCharacters, boolean truncated,
                             Map<String, Integer> appliedRuleCounts, int protectedLineCount, int repeatedBlockCount,
                             int omittedRepeatBlockCount, int repeatCompressionCharacters) {
    }

    public record Readiness(boolean ready, String guidanceCode, String guidance) {
    }

    private record TruncatedLog(String text, boolean truncated) {
    }

    private record CompiledRule(LogParsingRule rule, Pattern pattern) {
    }

    private record RuleApplication(String text, Map<String, Integer> appliedRuleCounts, int protectedLineCount) {
    }
}
