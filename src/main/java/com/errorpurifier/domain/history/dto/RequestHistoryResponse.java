package com.errorpurifier.domain.history.dto;

import com.errorpurifier.domain.history.entity.RequestHistory;

import java.time.LocalDateTime;

public record RequestHistoryResponse(Long id, String deviceId, Long cacheId, String requestType, int processingTimeMs,
                                     LocalDateTime createdAt) {
    public static RequestHistoryResponse from(RequestHistory history) {
        return new RequestHistoryResponse(history.getId(), history.getDevice().getId().toString(),
                history.getCache() == null ? null : history.getCache().getId(), history.getRequestType().name(),
                history.getProcessingTimeMs(), history.getCreatedAt());
    }
}
