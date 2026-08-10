package com.errorpurifier.domain.usage.entity;

import com.errorpurifier.domain.cache.entity.ErrorCache;
import com.errorpurifier.domain.client.entity.ClientDevice;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "llm_usage_log", indexes = {
        @Index(name = "idx_usage_created_at", columnList = "created_at"),
        @Index(name = "idx_usage_provider_model", columnList = "provider,model")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class LlmUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private ClientDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cache_id")
    private ErrorCache cache;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean cacheHit;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, length = 64)
    private String promptHash;

    @Column(nullable = false)
    private int originalCharacters;

    @Column(nullable = false)
    private int preparedCharacters;

    @Column(nullable = false)
    private int inputTokens;

    @Column(nullable = false)
    private int outputTokens;

    @Column(nullable = false)
    private int totalTokens;

    @Column(nullable = false)
    private long latencyMs;

    @Column(columnDefinition = "TEXT")
    private String referencedLines;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false)
    private boolean resolved;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public LlmUsageLog(ClientDevice device, ErrorCache cache, boolean cacheHit, String provider, String model, String promptHash,
                       int originalCharacters, int preparedCharacters, int inputTokens, int outputTokens,
                       int totalTokens, long latencyMs, String referencedLines, int rating) {
        this.device = device;
        this.cache = cache;
        this.cacheHit = cacheHit;
        this.provider = provider;
        this.model = model;
        this.promptHash = promptHash;
        this.originalCharacters = originalCharacters;
        this.preparedCharacters = preparedCharacters;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.latencyMs = latencyMs;
        this.referencedLines = referencedLines;
        this.rating = rating;
    }

    public boolean recordFeedback(int rating, boolean resolved) {
        if (this.rating != 0) {
            return false;
        }
        this.rating = rating;
        this.resolved = resolved;
        return true;
    }
}
