package com.errorpurifier.domain.rule.config;

import com.errorpurifier.domain.rule.entity.LogParsingRule;
import com.errorpurifier.domain.rule.repository.LogParsingRuleRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Seeds only an empty rule database; user-managed rules are never overwritten. */
@Configuration
public class DefaultLogParsingRuleInitializer {

    @Bean
    ApplicationRunner seedDefaultLogParsingRules(LogParsingRuleRepository repository) {
        return arguments -> {
            for (RuleDefinition rule : defaultRules()) {
                if (!repository.existsByDescription(rule.description())) {
                    repository.save(LogParsingRule.builder()
                            .ruleType(rule.type())
                            .targetFramework(rule.category())
                            .regexPattern(rule.regex())
                            .priority(rule.priority())
                            .description(rule.description())
                            .minPluginVersion("1.0.0")
                            .build());
                }
            }
        };
    }

    private List<RuleDefinition> defaultRules() {
        return List.of(
                rule("ANSI 색상 코드를 제거합니다.", "COMMON", "\\u001B\\[[;\\d]*m", 100, LogParsingRule.RuleType.BLACKLIST),
                rule("원인 분석에 기여하지 않는 Gradle 요약 행을 제거합니다.", "GRADLE", "(?m)^(?:> Task :.*|\\d+ actionable tasks:.*|BUILD (?:SUCCESSFUL|FAILED) in .*)$", 90, LogParsingRule.RuleType.BLACKLIST),
                rule("Gradle 경고 안내를 제거합니다.", "GRADLE", "(?m)^(?:.*Deprecated Gradle features were used.*|.*Use '--warning-mode all'.*|.*See https://docs\\.gradle\\.org.*|.*BUILD CACHE.*|Welcome to Gradle \\d.*)$", 88, LogParsingRule.RuleType.BLACKLIST),
                rule("Spring CGLIB 프록시 내부 호출 프레임 제거", "SPRING", "^\\s*at org\\.springframework\\.cglib\\..*$", 89, LogParsingRule.RuleType.BLACKLIST),
                rule("Spring AOP 어드바이스 체인 내부 프레임 제거", "SPRING", "^\\s*at org\\.springframework\\.aop\\..*$", 89, LogParsingRule.RuleType.BLACKLIST),
                rule("Spring MVC DispatcherServlet 내부 프레임 제거", "SPRING", "^\\s*at org\\.springframework\\.web\\.servlet\\..*$", 85, LogParsingRule.RuleType.BLACKLIST),
                rule("Spring 트랜잭션 내부 프레임 제거", "SPRING", "^\\s*at org\\.springframework\\.transaction\\..*$", 85, LogParsingRule.RuleType.BLACKLIST),
                rule("Spring Bean 팩토리 내부 프레임 제거", "SPRING", "^\\s*at org\\.springframework\\.beans\\.factory\\..*$", 80, LogParsingRule.RuleType.BLACKLIST),
                rule("Spring Boot 자동설정 내부 프레임 제거", "SPRING", "^\\s*at org\\.springframework\\.boot\\.autoconfigure\\..*$", 80, LogParsingRule.RuleType.BLACKLIST),
                rule("Java 리플렉션 프레임 제거", "REFLECTION", "^\\s*at (?:java\\.base/)?java\\.lang\\.reflect\\..*$", 95, LogParsingRule.RuleType.BLACKLIST),
                rule("JDK 내부 리플렉션 프레임 제거", "REFLECTION", "^\\s*at (?:java\\.base/)?jdk\\.internal\\.reflect\\..*$", 95, LogParsingRule.RuleType.BLACKLIST),
                rule("Kotlin 리플렉션 프레임 제거", "REFLECTION", "^\\s*at kotlin\\.reflect\\..*$", 90, LogParsingRule.RuleType.BLACKLIST),
                rule("JDK 동적 프록시 프레임 제거", "PROXY", "^\\s*at jdk\\.proxy\\d+\\..*$", 85, LogParsingRule.RuleType.BLACKLIST),
                rule("ByteBuddy 프록시 프레임 제거", "PROXY", "^\\s*at net\\.bytebuddy\\..*$", 85, LogParsingRule.RuleType.BLACKLIST),
                rule("Hibernate 내부 프레임 제거", "HIBERNATE", "^\\s*at org\\.hibernate\\.(?:engine|internal|action|proxy)\\..*$", 85, LogParsingRule.RuleType.BLACKLIST),
                rule("Tomcat Catalina 내부 프레임 제거", "TOMCAT", "^\\s*at org\\.apache\\.catalina\\..*$", 85, LogParsingRule.RuleType.BLACKLIST),
                rule("Tomcat Coyote 내부 프레임 제거", "TOMCAT", "^\\s*at org\\.apache\\.coyote\\..*$", 85, LogParsingRule.RuleType.BLACKLIST),
                rule("Netty 내부 프레임 제거", "NETTY_REACTOR", "^\\s*at io\\.netty\\..*$", 85, LogParsingRule.RuleType.BLACKLIST),
                rule("Reactor 내부 프레임 제거", "NETTY_REACTOR", "^\\s*at reactor\\.core\\..*$", 85, LogParsingRule.RuleType.BLACKLIST),
                rule("ThreadPoolExecutor 프레임 제거", "THREAD", "^\\s*at (?:java\\.base/)?java\\.util\\.concurrent\\.ThreadPoolExecutor.*$", 80, LogParsingRule.RuleType.BLACKLIST),
                rule("Thread.run 프레임 제거", "THREAD", "^\\s*at (?:java\\.base/)?java\\.lang\\.Thread\\.run\\(Thread\\.java:\\d+\\)$", 75, LogParsingRule.RuleType.BLACKLIST),
                rule("Maven Surefire 프레임 제거", "MAVEN", "^\\s*at org\\.apache\\.maven\\.surefire\\..*$", 80, LogParsingRule.RuleType.BLACKLIST),
                rule("JUnit 플랫폼 프레임 제거", "JUNIT", "^\\s*at org\\.junit\\.platform\\..*$", 80, LogParsingRule.RuleType.BLACKLIST),
                rule("메모리 주소 표기 제거", "COMMON", "@[0-9a-f]{6,8}\\b", 55, LogParsingRule.RuleType.BLACKLIST),
                rule("Caused by 행 보존", "COMMON", "^Caused by:.*$", 999, LogParsingRule.RuleType.WHITELIST),
                rule("예외 메시지 첫 행 보존", "COMMON", "^[\\w.$]+(?:Exception|Error):.*$", 999, LogParsingRule.RuleType.WHITELIST),
                rule("축약된 스택트레이스 행 보존", "COMMON", "^\\s*\\.\\.\\. \\d+ more$", 999, LogParsingRule.RuleType.WHITELIST)
        );
    }

    private RuleDefinition rule(String description, String category, String regex, int priority, LogParsingRule.RuleType type) {
        return new RuleDefinition(description, category, regex, priority, type);
    }

    private record RuleDefinition(String description, String category, String regex, int priority, LogParsingRule.RuleType type) {
    }
}
