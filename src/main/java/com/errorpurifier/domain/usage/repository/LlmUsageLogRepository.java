package com.errorpurifier.domain.usage.repository;

import com.errorpurifier.domain.usage.entity.LlmUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LlmUsageLogRepository extends JpaRepository<LlmUsageLog, Long> {
    long countByRating(int rating);
    long countByResolvedTrue();

    @Query("""
            select count(u) as totalRequests,
                   coalesce(sum(case when u.rating = 1 then 1 else 0 end), 0) as helpfulResponses,
                   coalesce(sum(case when u.rating = -1 then 1 else 0 end), 0) as unhelpfulResponses,
                   coalesce(sum(case when u.resolved = true then 1 else 0 end), 0) as resolvedResponses,
                   coalesce(sum(u.inputTokens), 0) as inputTokens,
                   coalesce(sum(u.outputTokens), 0) as outputTokens,
                   coalesce(sum(u.thinkingTokens), 0) as thinkingTokens,
                   coalesce(sum(u.totalTokens), 0) as totalTokens,
                   coalesce(sum(u.originalCharacters), 0) as originalCharacters,
                   coalesce(sum(u.preparedCharacters), 0) as preparedCharacters,
                   coalesce(avg(u.latencyMs), 0) as averageLatencyMs
            from LlmUsageLog u
            where u.device.id = :deviceId
            """)
    UsageSummaryProjection summarizeByDeviceId(@Param("deviceId") UUID deviceId);

    interface UsageSummaryProjection {
        long getTotalRequests();
        long getHelpfulResponses();
        long getUnhelpfulResponses();
        long getResolvedResponses();
        long getInputTokens();
        long getOutputTokens();
        long getThinkingTokens();
        long getTotalTokens();
        long getOriginalCharacters();
        long getPreparedCharacters();
        double getAverageLatencyMs();
    }
}
