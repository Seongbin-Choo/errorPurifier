package com.errorpurifier.domain.cache.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record CacheCheckResponse(
        boolean cacheHit,
        String cacheKey,
        String exceptionType,
        String preparedPrompt,
        int originalCharacters,
        int preparedCharacters,
        boolean analysisReady,
        String guidance,
        boolean logTruncated,
        Map<String, Integer> appliedRuleCounts,
        int protectedLineCount
) {
}
