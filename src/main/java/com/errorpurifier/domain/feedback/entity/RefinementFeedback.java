package com.errorpurifier.domain.feedback.entity;

import com.errorpurifier.domain.cache.entity.ErrorCache;
import com.errorpurifier.domain.client.entity.ClientDevice;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "refinement_feedback", indexes = {
        @Index(name = "idx_refinement_feedback_created", columnList = "created_at"),
        @Index(name = "idx_refinement_feedback_type", columnList = "feedback_type")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class RefinementFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private ClientDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cache_id")
    private ErrorCache cache;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 30)
    private RefinementFeedbackType feedbackType;

    @Column(nullable = false)
    private int originalCharacters;

    @Column(nullable = false)
    private int preparedCharacters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json", nullable = false)
    private Map<String, Integer> appliedRuleCounts;

    @Column(nullable = false)
    private int protectedLineCount;

    @Column(nullable = false)
    private boolean logTruncated;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RefinementFeedback(ClientDevice device, ErrorCache cache, RefinementFeedbackType feedbackType,
                              int originalCharacters, int preparedCharacters, Map<String, Integer> appliedRuleCounts,
                              int protectedLineCount, boolean logTruncated) {
        this.device = device;
        this.cache = cache;
        this.feedbackType = feedbackType;
        this.originalCharacters = originalCharacters;
        this.preparedCharacters = preparedCharacters;
        this.appliedRuleCounts = appliedRuleCounts;
        this.protectedLineCount = protectedLineCount;
        this.logTruncated = logTruncated;
    }
}
