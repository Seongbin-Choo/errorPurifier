package com.errorpurifier.domain.history.dto;

public record HistoryEvent(
        String deviceId,
        Long cacheId,
        String requestType,
        long processingTimeMs
) {}
