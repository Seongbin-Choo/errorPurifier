package com.errorpurifier;

import com.errorpurifier.domain.cache.entity.ErrorCache;
import com.errorpurifier.domain.cache.repository.ErrorCacheRepository;
import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UtcTimestampPersistenceIntegrationTest {

    @Autowired
    private ClientDeviceRepository clientDeviceRepository;

    @Autowired
    private ErrorCacheRepository errorCacheRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void enforceUtcBeforeTest() {
        ErrorPurifierApplication.configureUtcTimezone();
    }

    @AfterEach
    void maintainUtcAfterTest() {
        ErrorPurifierApplication.configureUtcTimezone();
    }

    @Test
    void persistsExplicitAndJpaAuditedTimestampsAsUtcDatetimes() {
        Instant earliestExpected = Instant.now().minusSeconds(5);

        UUID deviceId = UUID.randomUUID();
        clientDeviceRepository.saveAndFlush(ClientDevice.builder()
                .id(deviceId)
                .pluginVersion("1.0.0-test")
                .build());

        ErrorCache cache = errorCacheRepository.saveAndFlush(ErrorCache.builder()
                .cacheKey(UUID.randomUUID().toString().replace("-", ""))
                .environmentTags(Map.of("ide", "intellij"))
                .exceptionType("java.lang.IllegalStateException")
                .processTemplate("[refined log]")
                .savedTokens(10)
                .build());

        Long cacheId = cache.getId();
        entityManager.clear();

        ClientDevice persistedDevice = clientDeviceRepository.findById(deviceId).orElseThrow();
        ErrorCache persistedCache = errorCacheRepository.findById(cacheId).orElseThrow();
        LocalDateTime rawCacheCreatedAt = jdbcTemplate.queryForObject(
                "select created_at from error_cache where id = ?",
                LocalDateTime.class,
                cacheId
        );
        Instant latestExpected = Instant.now().plusSeconds(5);

        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
        assertUtcCurrentTime(persistedDevice.getCreatedAt(), earliestExpected, latestExpected);
        assertUtcCurrentTime(persistedDevice.getUpdatedAt(), earliestExpected, latestExpected);
        assertUtcCurrentTime(persistedCache.getCreatedAt(), earliestExpected, latestExpected);
        assertUtcCurrentTime(persistedCache.getUpdatedAt(), earliestExpected, latestExpected);
        assertThat(rawCacheCreatedAt).isEqualTo(persistedCache.getCreatedAt());
    }

    private void assertUtcCurrentTime(LocalDateTime timestamp, Instant earliestExpected, Instant latestExpected) {
        assertThat(timestamp).isNotNull();
        assertThat(timestamp.toInstant(ZoneOffset.UTC)).isBetween(earliestExpected, latestExpected);
    }
}
