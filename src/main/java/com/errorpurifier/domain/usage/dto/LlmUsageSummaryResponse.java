package com.errorpurifier.domain.usage.dto;

public record LlmUsageSummaryResponse(long totalRequests, long helpfulResponses, long unhelpfulResponses,
                                      long resolvedResponses, long inputTokens, long outputTokens, long totalTokens,
                                      long originalCharacters, long preparedCharacters, double promptCharacterChangePercent,
                                      long averageLatencyMs) {
}
