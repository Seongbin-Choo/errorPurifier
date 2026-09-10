package com.errorpurifier.benchmark;

import com.errorpurifier.domain.cache.service.LogPromptRefiner;

public final class LogRefinementMetrics {

    private LogRefinementMetrics() {
    }

    public static void main(String[] args) {
        LogPromptRefiner refiner = LogRefinementBenchmarkSupport.newRefiner();
        System.out.println("target_chars,input_chars,refined_chars,reduction_percent,repeat_blocks,omitted_blocks,repeat_saved_chars,truncated");
        for (int targetCharacters : LogRefinementBenchmarkSupport.INPUT_SIZES) {
            String rawLog = LogRefinementBenchmarkSupport.generateLog(targetCharacters);
            LogPromptRefiner.RefinedLog result = refiner.refine(
                    rawLog, null, LogRefinementBenchmarkSupport.PROJECT_TAGS);
            double reduction = 100.0 * (rawLog.length() - result.text().length()) / rawLog.length();
            System.out.printf(java.util.Locale.ROOT, "%d,%d,%d,%.2f,%d,%d,%d,%s%n",
                    targetCharacters,
                    rawLog.length(),
                    result.text().length(),
                    reduction,
                    result.repeatedBlockCount(),
                    result.omittedRepeatBlockCount(),
                    result.repeatCompressionCharacters(),
                    result.truncated());
        }
    }
}
