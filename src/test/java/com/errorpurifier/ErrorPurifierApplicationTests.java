package com.errorpurifier;

import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import com.errorpurifier.domain.cache.entity.ErrorCache;
import com.errorpurifier.domain.cache.repository.ErrorCacheRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class ErrorPurifierApplicationTests {

    @Autowired
    private ClientDeviceRepository clientDeviceRepository;

    @Autowired
    private ErrorCacheRepository errorCacheRepository;

    @Test
    void contextLoads() {
    }

    @Test
    @Transactional
    @DisplayName("UUID를 BINARY(16) client_device ID로 저장할 수 있다")
    void savesDeviceUuidAsBinary() {
        UUID deviceId = UUID.randomUUID();
        clientDeviceRepository.saveAndFlush(ClientDevice.builder()
                .id(deviceId)
                .pluginVersion("1.0.0")
                .build());

        assertThat(clientDeviceRepository.findById(deviceId)).isPresent();
    }

    @Test
    @Transactional
    @DisplayName("정제 프로세스 템플릿을 기존 error_cache 스키마에 저장할 수 있다")
    void savesProcessTemplate() {
        ErrorCache cache = errorCacheRepository.saveAndFlush(ErrorCache.builder()
                .cacheKey("a".repeat(64))
                .environmentTags(Map.of("ide", "intellij"))
                .exceptionType("java.lang.IllegalStateException")
                .processTemplate("[정제된 오류 로그]\\n{refined_log}")
                .savedTokens(0)
                .build());

        assertThat(cache.getId()).isNotNull();
        assertThat(errorCacheRepository.findById(cache.getId())).isPresent();
    }

}
