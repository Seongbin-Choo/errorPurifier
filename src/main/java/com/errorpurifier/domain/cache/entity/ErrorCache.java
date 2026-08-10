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

    // Hibernate 6 JSON 자동 매핑 (Spring Boot 3.x 필수)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private Map<String, String> environmentTags;

    @Column(nullable = false)
    private String exceptionType;

    // 기존 DB의 solution_text 열을 정제 프로세스 템플릿으로 재해석한다.
    // 운영 스키마 전환은 별도 마이그레이션으로 process_template 열로 rename한다.
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

    // 비즈니스 메서드들 (외부에서 Setter로 조작 금지)
    public void increaseHitCount() {
        this.hitCount++;
    }

    public void increaseSuccessCount() {
        this.successCount++;
    }

    public void reportError() {
        this.reportCount++;
        if (this.reportCount >= 3) { // 3회 이상 신고 시 자동 블라인드
            this.isBlinded = true;
        }
    }

    /**
     * A cache is reused only while feedback supports it. Two unhelpful reports
     * with fewer helpful reports are enough to stop automatic reuse; the row is
     * kept for later process improvement and is permanently blinded at 3 reports.
     */
    public boolean isReusable() {
        return !isBlinded && !(reportCount >= 2 && reportCount > successCount);
    }

    public void updateProcessTemplate(String processTemplate, int savedTokens) {
        this.processTemplate = processTemplate;
        this.savedTokens = savedTokens;
        this.isBlinded = false;
    }
}
