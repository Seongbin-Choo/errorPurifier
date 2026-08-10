package com.errorpurifier.domain.history.dto;

public record HistoryEvent(
        String deviceId,
        Long cacheId,
        String requestType, // "CACHE_HIT" or "LLM_CALL"
        long processingTimeMs
) {}