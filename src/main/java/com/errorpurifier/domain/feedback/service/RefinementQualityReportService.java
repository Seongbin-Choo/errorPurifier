package com.errorpurifier.domain.feedback.service;

import com.errorpurifier.domain.feedback.dto.RefinementQualitySummaryResponse;
import com.errorpurifier.domain.feedback.entity.RefinementFeedback;
import com.errorpurifier.domain.feedback.entity.RefinementFeedbackType;
import com.errorpurifier.domain.feedback.repository.RefinementFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefinementQualityReportService {
    private final RefinementFeedbackRepository feedbackRepository;

    @Transactional(readOnly = true)
    public RefinementQualitySummaryResponse summary() {
        List<RefinementFeedback> feedback = feedbackRepository.findAll();
        Map<String, MutableCategoryQuality> categories = new LinkedHashMap<>();
        for (RefinementFeedback item : feedback) {
            item.getAppliedRuleCounts().forEach((category, appliedLines) -> categories
                    .computeIfAbsent(category, ignored -> new MutableCategoryQuality())
                    .add(item.getFeedbackType(), appliedLines));
        }
        List<RefinementQualitySummaryResponse.CategoryQuality> topCategories = categories.entrySet().stream()
                .map(entry -> entry.getValue().toResponse(entry.getKey()))
                .sorted(Comparator.comparingLong(RefinementQualitySummaryResponse.CategoryQuality::negativeFeedback).reversed()
                        .thenComparing(Comparator.comparingLong(RefinementQualitySummaryResponse.CategoryQuality::feedbackCount).reversed()))
                .limit(5)
                .toList();
        return new RefinementQualitySummaryResponse(
                breakdown(feedback),
                breakdown(feedback.stream().filter(RefinementFeedback::isLogTruncated).toList()),
                breakdown(feedback.stream().filter(item -> !item.isLogTruncated()).toList()),
                topCategories);
    }

    private RefinementQualitySummaryResponse.FeedbackBreakdown breakdown(List<RefinementFeedback> feedback) {
        return new RefinementQualitySummaryResponse.FeedbackBreakdown(
                feedback.size(),
                feedback.stream().filter(item -> item.getFeedbackType() == RefinementFeedbackType.APPROPRIATE).count(),
                feedback.stream().filter(item -> item.getFeedbackType() == RefinementFeedbackType.MISSING_CONTEXT).count(),
                feedback.stream().filter(item -> item.getFeedbackType() == RefinementFeedbackType.TOO_NOISY).count());
    }

    private static class MutableCategoryQuality {
        private long feedbackCount;
        private long appliedLines;
        private long appropriate;
        private long missingContext;
        private long tooNoisy;

        void add(RefinementFeedbackType feedbackType, int appliedLineCount) {
            feedbackCount++;
            appliedLines += appliedLineCount;
            switch (feedbackType) {
                case APPROPRIATE -> appropriate++;
                case MISSING_CONTEXT -> missingContext++;
                case TOO_NOISY -> tooNoisy++;
            }
        }

        RefinementQualitySummaryResponse.CategoryQuality toResponse(String category) {
            return new RefinementQualitySummaryResponse.CategoryQuality(category, feedbackCount, appliedLines,
                    appropriate, missingContext, tooNoisy);
        }
    }
}
