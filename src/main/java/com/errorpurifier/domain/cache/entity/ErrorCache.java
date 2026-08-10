package com.errorpurifier.domain.cache.entity;

import com.errorpurifier.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "error_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorCache extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String cacheKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private Map<String, String> environmentTags;

    @Column(nullable = false)
    private String exceptionType;

    @Column(name = "solution_text", nullable = false, columnDefinition = "TEXT")
    private String processTemplate;

    @Column(nullable = false)
    private int savedTokens;

    @Column(nullable = false)
    private int hitCount = 0;

    @Column(nullable = false)
    private int successCount = 0;

    @Column(nullable = false)
    private int reportCount = 0;

    @Column(nullable = false)
    private boolean isBlinded = false;

    @Version
    private Long version;

    @Builder
    public ErrorCache(String cacheKey, Map<String, String> environmentTags, String exceptionType, String processTemplate, int savedTokens) {
        this.cacheKey = cacheKey;
        this.environmentTags = environmentTags;
        this.exceptionType = exceptionType;
        this.processTemplate = processTemplate;
        this.savedTokens = savedTokens;
    }

    public void increaseHitCount() {
        this.hitCount++;
    }

    public void increaseSuccessCount() {
        this.successCount++;
    }

    public void reportError() {
        this.reportCount++;
        if (this.reportCount >= 3) {
            this.isBlinded = true;
        }
    }

    public boolean isReusable() {
        return !isBlinded && !(reportCount >= 2 && reportCount > successCount);
    }

    public void updateProcessTemplate(String processTemplate, int savedTokens) {
        this.processTemplate = processTemplate;
        this.savedTokens = savedTokens;
        this.isBlinded = false;
    }
}
