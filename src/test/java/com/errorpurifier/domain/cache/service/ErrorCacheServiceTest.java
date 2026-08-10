package com.errorpurifier.domain.cache.service;

import com.errorpurifier.domain.cache.dto.CacheCheckRequest;
import com.errorpurifier.domain.cache.dto.CacheCheckResponse;
import com.errorpurifier.domain.cache.entity.ErrorCache;
import com.errorpurifier.domain.cache.repository.ErrorCacheRepository;
import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import com.errorpurifier.domain.client.service.DeviceRequestLimiter;
import com.errorpurifier.domain.client.service.RateLimitProperties;
import com.errorpurifier.domain.history.dto.HistoryEvent;
import com.errorpurifier.domain.knowledge.entity.DiagnosticPlaybook;
import com.errorpurifier.domain.knowledge.repository.DiagnosticPlaybookRepository;
import com.errorpurifier.domain.knowledge.service.DiagnosticPlaybookMatcher;
import com.errorpurifier.domain.rule.repository.LogParsingRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorCacheServiceTest {

    private ErrorCacheRepository cacheRepository;
    private ClientDeviceRepository deviceRepository;
    private ApplicationEventPublisher eventPublisher;
    private DiagnosticPlaybookRepository playbookRepository;
    private ErrorCacheService service;
    private UUID deviceId;

    @BeforeEach
    void setUp() {
        cacheRepository = mock(ErrorCacheRepository.class);
        deviceRepository = mock(ClientDeviceRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        playbookRepository = mock(DiagnosticPlaybookRepository.class);
        when(playbookRepository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(java.util.List.of());
        LogParsingRuleRepository ruleRepository = mock(LogParsingRuleRepository.class);
        when(ruleRepository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(java.util.List.of());
        RateLimitProperties rateLimitProperties = new RateLimitProperties();
        service = new ErrorCacheService(
                cacheRepository,
                deviceRepository,
                new DeviceRequestLimiter(rateLimitProperties),
                new LogPromptRefiner(ruleRepository, new SensitiveDataSanitizer(), new RepeatedLogCompressor()),
                new ProjectContextExtractor(),
                new DiagnosticPlaybookMatcher(playbookRepository),
                eventPublisher
        );
        deviceId = UUID.randomUUID();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(ClientDevice.builder()
                .id(deviceId)
                .pluginVersion("1.0.0")
                .build()));
    }

    @Test
    void cacheMissCreatesDefaultProcessAndReturnsPreparedPrompt() {
        when(cacheRepository.findByCacheKeyAndIsBlindedFalse(anyString())).thenReturn(Optional.empty());
        when(cacheRepository.save(any(ErrorCache.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CacheCheckResponse response = service.preparePrompt(request(), deviceId.toString());

        assertThat(response.cacheHit()).isFalse();
        assertThat(response.exceptionType()).isEqualTo("java.lang.IllegalStateException");
        assertThat(response.preparedPrompt()).contains("build-tool: gradle", "java: 21", "IllegalStateException", "Sample.java:42");
        assertThat(response.preparedPrompt()).doesNotContain("Sample.java:#");
        assertThat(response.cacheKey()).hasSize(64);
        verify(cacheRepository).save(any(ErrorCache.class));
        ArgumentCaptor<HistoryEvent> event = ArgumentCaptor.forClass(HistoryEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().requestType()).isEqualTo("LLM_CALL");
    }

    @Test
    void cacheHitUsesStoredProcessTemplate() {
        ErrorCache cache = ErrorCache.builder()
                .cacheKey("a".repeat(64))
                .environmentTags(Map.of("build-tool", "gradle"))
                .exceptionType("java.lang.IllegalStateException")
                .processTemplate("환경={project_context}\n로그={refined_log}")
                .savedTokens(120)
                .build();
        when(cacheRepository.findByCacheKeyAndIsBlindedFalse(anyString())).thenReturn(Optional.of(cache));

        CacheCheckResponse response = service.preparePrompt(request(), deviceId.toString());

        assertThat(response.cacheHit()).isTrue();
        assertThat(response.preparedPrompt()).contains("환경=", "로그=");
        assertThat(cache.getHitCount()).isEqualTo(1);
    }

    @Test
    void cacheWithRepeatedUnhelpfulFeedbackIsBypassedWithoutCreatingDuplicate() {
        ErrorCache cache = ErrorCache.builder()
                .cacheKey("b".repeat(64))
                .environmentTags(Map.of())
                .exceptionType("java.lang.IllegalStateException")
                .processTemplate("stored={refined_log}")
                .savedTokens(10)
                .build();
        cache.reportError();
        cache.reportError();
        when(cacheRepository.findByCacheKeyAndIsBlindedFalse(anyString())).thenReturn(Optional.of(cache));

        CacheCheckResponse response = service.preparePrompt(request(), deviceId.toString());

        assertThat(response.cacheHit()).isFalse();
        assertThat(response.preparedPrompt()).contains("개발자 콘솔에서 추출·정제한 오류 정보");
        assertThat(cache.getHitCount()).isZero();
        verify(cacheRepository, never()).save(any(ErrorCache.class));
    }

    @Test
    void gradleWrapperFailureWithoutCauseDoesNotCallLlmOrCreateCache() {
        CacheCheckResponse response = service.preparePrompt(new CacheCheckRequest(
                "Execution failed for task ':app.run()'.\nProcess finished with non-zero exit value 1\nBUILD FAILED",
                null, Map.of(), Map.of()), deviceId.toString());

        assertThat(response.analysisReady()).isFalse();
        assertThat(response.guidance()).contains("--stacktrace");
        verifyNoInteractions(cacheRepository);
    }

    @Test
    void incompleteSelectionFallsBackToRootCauseInFullConsole() {
        CacheCheckResponse response = service.preparePrompt(new CacheCheckRequest(
                "java.lang.IllegalStateException: database connection failed\n\tat com.example.App.main(App.java:20)\n"
                        + "Execution failed for task ':app.run()'.\nBUILD FAILED",
                "Execution failed for task ':app.run()'.\nProcess finished with non-zero exit value 1\nBUILD FAILED",
                Map.of(), Map.of()), deviceId.toString());

        assertThat(response.analysisReady()).isTrue();
        assertThat(response.preparedPrompt()).contains("database connection failed");
    }

    @Test
    void longLogKeepsExceptionContextWithinPromptBudget() {
        String longLog = "debug line\n".repeat(3_000)
                + "java.lang.IllegalStateException: root cause\n"
                + "at com.example.App.run(App.java:42)\n"
                + "trailing line\n".repeat(1_000);
        when(cacheRepository.findByCacheKeyAndIsBlindedFalse(anyString())).thenReturn(Optional.empty());
        when(cacheRepository.findByCacheKey(anyString())).thenReturn(Optional.empty());
        when(cacheRepository.save(any(ErrorCache.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CacheCheckResponse response = service.preparePrompt(new CacheCheckRequest(longLog, null, Map.of(), Map.of()), deviceId.toString());

        assertThat(response.logTruncated()).isTrue();
        assertThat(response.preparedPrompt()).contains("IllegalStateException: root cause", "긴 로그의 중간 구간 생략");
    }

    @Test
    void addsALombokHintForMissingGeneratedMembers() {
        when(cacheRepository.findByCacheKeyAndIsBlindedFalse(anyString())).thenReturn(Optional.empty());
        when(cacheRepository.findByCacheKey(anyString())).thenReturn(Optional.empty());
        when(cacheRepository.save(any(ErrorCache.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(playbookRepository.findByIsActiveTrueOrderByPriorityDesc()).thenReturn(java.util.List.of(
                DiagnosticPlaybook.builder().name("LOMBOK_ANNOTATION_PROCESSING")
                        .matchPattern("(?s)cannot find symbol.*getName")
                        .guidance("Lombok 어노테이션 처리 문제를 우선 점검하세요. Annotation Processing을 확인하세요.")
                        .priority(100).build()));

        CacheCheckResponse response = service.preparePrompt(new CacheCheckRequest(
                "error: cannot find symbol\n  symbol: method getName()\n  location: variable response\n",
                null, Map.of("build.gradle", "compileOnly 'org.projectlombok:lombok'"), Map.of()), deviceId.toString());

        assertThat(response.preparedPrompt()).contains("Lombok 어노테이션 처리 문제", "Annotation Processing");
    }

    private CacheCheckRequest request() {
        return new CacheCheckRequest(
                "java.lang.IllegalStateException: boom\n\tat com.example.Sample.run(Sample.java:42)",
                null,
                Map.of("build.gradle", "java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }"),
                Map.of()
        );
    }
}
