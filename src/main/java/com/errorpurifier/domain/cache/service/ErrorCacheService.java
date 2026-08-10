package com.errorpurifier.domain.cache.service;

import com.errorpurifier.domain.cache.dto.CacheCheckRequest;
import com.errorpurifier.domain.cache.dto.CacheCheckResponse;
import com.errorpurifier.domain.cache.dto.CacheContributeRequest;
import com.errorpurifier.domain.cache.entity.ErrorCache;
import com.errorpurifier.domain.cache.repository.ErrorCacheRepository;
import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.entity.DeviceStatus;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import com.errorpurifier.domain.client.service.DeviceRequestLimiter;
import com.errorpurifier.domain.history.dto.HistoryEvent;
import com.errorpurifier.domain.knowledge.service.DiagnosticPlaybookMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorCacheService {

    private static final String DEFAULT_PROCESS_TEMPLATE = """
            다음은 개발자 콘솔에서 추출·정제한 오류 정보입니다.
            제공된 프로젝트 환경을 전제로 원인, 확인 방법, 수정안을 한국어로 설명하세요.
            추측이 필요한 경우에는 추측임을 분명히 밝히고, 로그에 없는 민감정보는 요구하지 마세요.
            답변 마지막에 실제 판단에 사용한 로그 줄을 `근거 로그: [L001, L002]` 형식으로 적으세요.

            [프로젝트 환경]
            {project_context}

            [정제된 오류 로그]
            {refined_log}
            """;

    private final ErrorCacheRepository errorCacheRepository;
    private final ClientDeviceRepository clientDeviceRepository;
    private final DeviceRequestLimiter deviceRequestLimiter;
    private final LogPromptRefiner logPromptRefiner;
    private final ProjectContextExtractor projectContextExtractor;
    private final DiagnosticPlaybookMatcher diagnosticPlaybookMatcher;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CacheCheckResponse preparePrompt(CacheCheckRequest request, String deviceUuidString) {
        ClientDevice device = getActiveDevice(deviceUuidString, true);
        long startedAt = System.nanoTime();

        Map<String, String> projectTags = projectContextExtractor.extract(request.projectFiles(), request.environmentTags());
        LogPromptRefiner.RefinedLog refinedLog = logPromptRefiner.refine(request.rawLog(), request.selectedText(), projectTags);
        List<DiagnosticPlaybookMatcher.DiagnosticPlaybookMatch> playbookMatches = diagnosticPlaybookMatcher.findMatches(refinedLog.text());
        List<String> knownErrorHints = playbookMatches.stream()
                .map(match -> "[" + match.name() + "] " + match.guidance())
                .toList();
        List<String> diagnosticPlaybooks = playbookMatches.stream()
                .map(DiagnosticPlaybookMatcher.DiagnosticPlaybookMatch::name)
                .toList();
        String cacheKey = createCacheKey(refinedLog.text(), projectTags);
        if (!refinedLog.readiness().ready()) {
            return new CacheCheckResponse(false, cacheKey, refinedLog.exceptionType(), null,
                    refinedLog.sourceCharacters(),
                    refinedLog.text().length(),
                    refinedLog.text().length(), false, refinedLog.readiness().guidance(), refinedLog.truncated(),
                    refinedLog.appliedRuleCounts(), refinedLog.protectedLineCount(), diagnosticPlaybooks);
        }
        ErrorCache cache = errorCacheRepository.findByCacheKeyAndIsBlindedFalse(cacheKey).orElse(null);
        boolean cacheHit = cache != null && cache.isReusable();

        String processTemplate;
        if (cacheHit) {
            processTemplate = cache.getProcessTemplate();
            cache.increaseHitCount();
        } else {
            processTemplate = DEFAULT_PROCESS_TEMPLATE;
            if (cache == null && errorCacheRepository.findByCacheKey(cacheKey).isEmpty()) {
                cache = errorCacheRepository.save(ErrorCache.builder()
                        .cacheKey(cacheKey)
                        .environmentTags(projectTags)
                        .exceptionType(refinedLog.exceptionType())
                        .processTemplate(processTemplate)
                        .savedTokens(0)
                        .build());
            }
        }
        String preparedPrompt = renderPrompt(processTemplate, refinedLog.text(), projectTags, knownErrorHints);
        diagnosticPlaybookMatcher.recordMatches(playbookMatches);

        publishHistoryEvent(device.getId().toString(), cacheHit ? cache.getId() : null,
                cacheHit ? "CACHE_HIT" : "LLM_CALL", elapsedMillis(startedAt));

        return new CacheCheckResponse(
                cacheHit,
                cacheKey,
                refinedLog.exceptionType(),
                preparedPrompt,
                refinedLog.sourceCharacters(),
                refinedLog.text().length(),
                preparedPrompt.length(),
                true,
                null,
                refinedLog.truncated(),
                refinedLog.appliedRuleCounts(),
                refinedLog.protectedLineCount(),
                diagnosticPlaybooks
        );
    }

    @Transactional
    public void contributeProcess(CacheContributeRequest request, String deviceUuidString) {
        getActiveDevice(deviceUuidString, false);
        errorCacheRepository.findByCacheKeyAndIsBlindedFalse(request.getCacheKey())
                .ifPresentOrElse(
                        cache -> cache.updateProcessTemplate(request.getProcessTemplate(), request.getSavedTokens()),
                        () -> log.warn("존재하지 않는 캐시 키에 대한 프로세스 등록 요청: {}", request.getCacheKey())
                );
    }

    private ClientDevice getActiveDevice(String deviceUuidString, boolean consumePromptQuota) {
        UUID deviceUuid;
        try {
            deviceUuid = UUID.fromString(deviceUuidString);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Device-UUID 형식이 올바르지 않습니다.");
        }

        ClientDevice device = clientDeviceRepository.findById(deviceUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "등록되지 않은 디바이스입니다. 먼저 동기화하세요."));
        if (device.getStatus() != DeviceStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "현재 디바이스는 요청을 수행할 수 없습니다.");
        }
        if (consumePromptQuota) {
            deviceRequestLimiter.verify(device.getId(), device.getDailyRequestCount());
            device.recordAccess();
        }
        return device;
    }

    private String createCacheKey(String refinedLog, Map<String, String> projectTags) {
        StringBuilder material = new StringBuilder(logPromptRefiner.normalizeForFingerprint(refinedLog));
        projectTags.forEach((key, value) -> material.append('\n').append(key.toLowerCase(Locale.ROOT)).append('=').append(value));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private String renderPrompt(String template, String refinedLog, Map<String, String> projectTags, List<String> knownErrorHints) {
        String numberedLog = numberLines(refinedLog);
        String rendered = template
                .replace("{project_context}", projectContextExtractor.asPromptContext(projectTags))
                .replace("{refined_log}", numberedLog);
        if (!template.contains("{refined_log}")) {
            rendered += "\n\n[정제된 오류 로그]\n" + numberedLog;
        }
        if (!rendered.contains("근거 로그:")) {
            rendered += "\n\n답변 마지막에 실제 판단에 사용한 로그 줄을 `근거 로그: [L001, L002]` 형식으로 적으세요.";
        }
        rendered += "\n\n`Caused by:`와 `Suppressed:`로 시작하는 예외는 누락하지 마세요. "
                + "각 예외가 근본 원인인지, 2차 증상인지 구분하고 근거 로그 줄을 함께 제시하세요.";
        if (!knownErrorHints.isEmpty()) {
            rendered += "\n\n[우선 점검 항목]\n" + String.join("\n", knownErrorHints);
        }
        return rendered;
    }

    private String numberLines(String refinedLog) {
        String[] lines = refinedLog.split("\\R");
        StringBuilder numbered = new StringBuilder();
        for (int index = 0; index < lines.length; index++) {
            numbered.append(String.format("L%03d | %s", index + 1, lines[index]));
            if (index < lines.length - 1) {
                numbered.append('\n');
            }
        }
        return numbered.toString();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private void publishHistoryEvent(String deviceId, Long cacheId, String requestType, long processingTime) {
        eventPublisher.publishEvent(new HistoryEvent(deviceId, cacheId, requestType, processingTime));
    }
}
