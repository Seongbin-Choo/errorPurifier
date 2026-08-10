package com.errorpurifier.domain.dashboard.dto;

import com.errorpurifier.domain.feedback.dto.RefinementQualitySummaryResponse;

import java.util.List;

public record AdminDashboardResponse(
        UsageOverview usage,
        CacheOverview cache,
        List<PlaybookUsage> topPlaybooks,
        RefinementQualitySummaryResponse refinementQuality
) {
    public record UsageOverview(long totalRequests, long cacheHitRequests, double cacheHitRatePercent,
                                long originalCharacters, long preparedCharacters, long repeatCompressionCharacters,
                                double promptCharacterChangePercent, long averageLatencyMs) {
    }

    public record CacheOverview(long cacheEntries) {
    }

    public record PlaybookUsage(String name, long matchCount, boolean active) {
    }
}
