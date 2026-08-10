package com.errorpurifier.domain.dashboard.service;

import com.errorpurifier.domain.cache.repository.ErrorCacheRepository;
import com.errorpurifier.domain.dashboard.dto.AdminDashboardResponse;
import com.errorpurifier.domain.feedback.service.RefinementQualityReportService;
import com.errorpurifier.domain.knowledge.repository.DiagnosticPlaybookRepository;
import com.errorpurifier.domain.usage.repository.LlmUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final LlmUsageLogRepository usageLogRepository;
    private final ErrorCacheRepository errorCacheRepository;
    private final DiagnosticPlaybookRepository playbookRepository;
    private final RefinementQualityReportService refinementQualityReportService;

    @Transactional(readOnly = true)
    public AdminDashboardResponse summary() {
        LlmUsageLogRepository.UsageSummaryProjection usage = usageLogRepository.summarizeAll();
        long totalRequests = usage.getTotalRequests();
        long originalCharacters = usage.getOriginalCharacters();
        long preparedCharacters = usage.getPreparedCharacters();
        double cacheHitRate = totalRequests == 0 ? 0D
                : Math.round(usage.getCacheHitRequests() * 1000D / totalRequests) / 10D;
        double promptCharacterChange = originalCharacters == 0 ? 0D
                : Math.round((preparedCharacters - originalCharacters) * 1000D / originalCharacters) / 10D;

        return new AdminDashboardResponse(
                new AdminDashboardResponse.UsageOverview(totalRequests, usage.getCacheHitRequests(), cacheHitRate,
                        originalCharacters, preparedCharacters, usage.getRepeatCompressionCharacters(), promptCharacterChange,
                        Math.round(usage.getAverageLatencyMs())),
                new AdminDashboardResponse.CacheOverview(errorCacheRepository.countByIsBlindedFalse()),
                playbookRepository.findTop5ByOrderByMatchCountDescIdAsc().stream()
                        .map(playbook -> new AdminDashboardResponse.PlaybookUsage(playbook.getName(), playbook.getMatchCount(), playbook.isActive()))
                        .toList(),
                refinementQualityReportService.summary()
        );
    }
}
