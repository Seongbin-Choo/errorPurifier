package com.errorpurifier.domain.cache.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RepeatedLogCompressorTest {

    @Test
    void preservesTheFirstTwoAndLastRetryBlocksWhileCompressingLongRuns() {
        RepeatedLogCompressor compressor = new RepeatedLogCompressor();
        StringBuilder log = new StringBuilder();
        for (int attempt = 1; attempt <= 47; attempt++) {
            appendRedisRetry(log, attempt, 47);
        }

        RepeatedLogCompressor.CompressionResult result = compressor.compress(log.toString().trim());

        assertThat(result.repeatedBlockCount()).isEqualTo(47);
        assertThat(result.omittedBlockCount()).isEqualTo(44);
        assertThat(result.savedCharacters()).isGreaterThan(5_000);
        assertThat(result.text())
                .contains("attempt 1/47")
                .contains("attempt 2/47")
                .contains("반복 로그 블록 47회 중 44회 생략")
                .contains("attempt 47/47")
                .doesNotContain("attempt 3/47");
    }

    @Test
    void compressesIndependentRetryRunsWithoutMergingDifferentFailures() {
        RepeatedLogCompressor compressor = new RepeatedLogCompressor();
        StringBuilder log = new StringBuilder();
        for (int attempt = 1; attempt <= 5; attempt++) {
            appendRedisRetry(log, attempt, 5);
        }
        for (int attempt = 1; attempt <= 6; attempt++) {
            appendKafkaRetry(log, attempt, 6);
        }

        RepeatedLogCompressor.CompressionResult result = compressor.compress(log.toString().trim());

        assertThat(result.repeatedBlockCount()).isEqualTo(11);
        assertThat(result.omittedBlockCount()).isEqualTo(5);
        assertThat(result.text())
                .contains("Redis connection retry")
                .contains("Kafka publish retry")
                .contains("반복 로그 블록 5회 중 2회 생략")
                .contains("반복 로그 블록 6회 중 3회 생략");
    }

    @Test
    void compressesRepeatedExceptionBlocksWithoutTimestampOrLogLevel() {
        RepeatedLogCompressor compressor = new RepeatedLogCompressor();
        StringBuilder log = new StringBuilder();
        for (int attempt = 1; attempt <= 6; attempt++) {
            log.append("java.lang.IllegalStateException: retry failure ").append(attempt).append('\n')
                    .append("\tat com.example.RetryService.execute(RetryService.java:42)\n");
        }

        RepeatedLogCompressor.CompressionResult result = compressor.compress(log.toString().trim());

        assertThat(result.repeatedBlockCount()).isEqualTo(6);
        assertThat(result.omittedBlockCount()).isEqualTo(3);
        assertThat(result.text())
                .contains("retry failure 1")
                .contains("retry failure 2")
                .contains("반복 로그 블록 6회 중 3회 생략")
                .contains("retry failure 6")
                .doesNotContain("retry failure 3");
    }

    private void appendRedisRetry(StringBuilder log, int attempt, int total) {
        log.append("2026-08-10 14:02:").append(String.format("%02d", attempt))
                .append(".221 [redis-retry-").append(attempt).append("] WARN Redis connection retry attempt ")
                .append(attempt).append('/').append(total).append(" after 5000ms\n")
                .append("io.lettuce.core.RedisConnectionException: Unable to connect to redis.internal:6379\n")
                .append("\tat com.example.RedisClient.reconnect(RedisClient.java:42)\n");
    }

    private void appendKafkaRetry(StringBuilder log, int attempt, int total) {
        log.append("2026-08-10 14:03:").append(String.format("%02d", attempt))
                .append(".221 [kafka-retry-").append(attempt).append("] WARN Kafka publish retry attempt ")
                .append(attempt).append('/').append(total).append(" after 3000ms\n")
                .append("org.apache.kafka.common.errors.TimeoutException: Topic publish timed out\n")
                .append("\tat com.example.KafkaPublisher.publish(KafkaPublisher.java:88)\n");
    }
}
