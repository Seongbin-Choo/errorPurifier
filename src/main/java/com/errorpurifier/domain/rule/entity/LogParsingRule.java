package com.errorpurifier.domain.rule.entity;

import com.errorpurifier.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "log_parsing_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogParsingRule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleType ruleType;

    @Column(nullable = false, length = 50)
    private String targetFramework;

    @Column(nullable = false, length = 500)
    private String regexPattern;

    @Column(nullable = false)
    private int priority;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 20)
    private String minPluginVersion;

    @Column(nullable = false)
    private boolean isActive;

    public enum RuleType {
        EXTRACT, BLACKLIST, WHITELIST
    }

    @Builder
    public LogParsingRule(RuleType ruleType, String targetFramework, String regexPattern, int priority, String description, String minPluginVersion) {
        this.ruleType = ruleType;
        this.targetFramework = targetFramework;
        this.regexPattern = regexPattern;
        this.priority = priority;
        this.description = description;
        this.minPluginVersion = minPluginVersion != null ? minPluginVersion : "1.0.0";
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void update(RuleType ruleType, String targetFramework, String regexPattern, int priority,
                       String description, String minPluginVersion) {
        this.ruleType = ruleType;
        this.targetFramework = targetFramework;
        this.regexPattern = regexPattern;
        this.priority = priority;
        this.description = description;
        this.minPluginVersion = minPluginVersion;
    }

    public void activate() {
        this.isActive = true;
    }
}
