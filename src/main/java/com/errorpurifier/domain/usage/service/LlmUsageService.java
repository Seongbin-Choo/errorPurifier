package com.errorpurifier.domain.usage.service;

import com.errorpurifier.domain.cache.repository.ErrorCacheRepository;
import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import com.errorpurifier.domain.usage.dto.LlmUsageRequest;
import com.errorpurifier.domain.usage.dto.LlmUsageResponse;
import com.errorpurifier.domain.usage.dto.LlmFeedbackRequest;
import com.errorpurifier.domain.usage.dto.LlmUsageSummaryResponse;
import com.errorpurifier.domain.usage.entity.LlmUsageLog;
import com.errorpurifier.domain.usage.repository.LlmUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LlmUsageService {
    private final LlmUsageLogRepository usageLogRepository;
    private final ClientDeviceRepository deviceRepository;
    private final ErrorCacheRepository cacheRepository;

    @Transactional
    public LlmUsageResponse record(String deviceId, LlmUsageRequest request) {
        ClientDevice device = getDevice(deviceId);

        LlmUsageLog usageLog = usageLogRepository.save(LlmUsageLog.builder()
                .device(device)
                // A miss uses the default process; its rating must not penalize a stored cache row.
                .cache(request.cacheHit() ? cacheRepository.findByCacheKeyAndIsBlindedFalse(request.cacheKey()).orElse(null) : null)
                .cacheHit(request.cacheHit())
                .provider(request.provider())
                .model(request.model())
                .promptHash(request.promptHash())
                .originalCharacters(request.originalCharacters())
                .preparedCharacters(request.preparedCharacters())
                .inputTokens(request.inputTokens())
                .outputTokens(request.outputTokens())
                .totalTokens(request.totalTokens())
                .latencyMs(request.latencyMs())
                .referencedLines(String.join(",", request.referencedLines() == null ? java.util.List.of() : request.referencedLines()))
                .rating(request.rating())
                .build());
        return new LlmUsageResponse(usageLog.getId());
    }

    @Transactional
    public void recordFeedback(String deviceId, Long usageId, LlmFeedbackRequest request) {
        ClientDevice device = getDevice(deviceId);
        LlmUsageLog usageLog = usageLogRepository.findById(usageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LLM 실행 기록을 찾을 수 없습니다."));
        if (!usageLog.getDevice().getId().equals(device.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 디바이스의 실행 기록에는 피드백을 남길 수 없습니다.");
        }
        if (!usageLog.recordFeedback(request.rating(), request.resolved())) {
            return;
        }
        if (!usageLog.isCacheHit() || usageLog.getCache() == null) {
            return;
        }
        if (request.rating() > 0) {
            usageLog.getCache().increaseSuccessCount();
        } else if (request.rating() < 0) {
            usageLog.getCache().reportError();
        }
    }

    @Transactional(readOnly = true)
    public LlmUsageSummaryResponse summary(String deviceId) {
        ClientDevice device = getDevice(deviceId);
        LlmUsageLogRepository.UsageSummaryProjection summary = usageLogRepository.summarizeByDeviceId(device.getId());
        long originalCharacters = summary.getOriginalCharacters();
        long preparedCharacters = summary.getPreparedCharacters();
        double characterChange = originalCharacters == 0 ? 0D
                : Math.round(((preparedCharacters - originalCharacters) * 1000D / originalCharacters)) / 10D;
        return new LlmUsageSummaryResponse(
                summary.getTotalRequests(), summary.getHelpfulResponses(), summary.getUnhelpfulResponses(), summary.getResolvedResponses(),
                summary.getInputTokens(), summary.getOutputTokens(), summary.getTotalTokens(),
                originalCharacters, preparedCharacters, characterChange, Math.round(summary.getAverageLatencyMs()));
    }

    private ClientDevice getDevice(String deviceId) {
        try {
            return deviceRepository.findById(UUID.fromString(deviceId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "등록되지 않은 디바이스입니다."));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Device-UUID 형식이 올바르지 않습니다.");
        }
    }
}
