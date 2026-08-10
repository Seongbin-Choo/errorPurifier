package com.errorpurifier.domain.usage.dto;

/** Device-local aggregate. Neither API keys, raw logs nor LLM response bodies are included. */
public record LlmUsageSummaryResponse(long totalRequests, long helpfulResponses, long unhelpfulResponses,
                                      long resolvedResponses, long inputTokens, long outputTokens, long totalTokens,
                                      long originalCharacters, long preparedCharacters, double promptCharacterChangePercent,
                                      long averageLatencyMs) {
}
