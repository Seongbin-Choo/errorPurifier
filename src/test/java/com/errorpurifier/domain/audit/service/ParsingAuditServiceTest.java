package com.errorpurifier.domain.audit.service;

import com.errorpurifier.domain.audit.dto.ParsingAuditRequest;
import com.errorpurifier.domain.audit.entity.ParsingAuditLog;
import com.errorpurifier.domain.audit.repository.ParsingAuditRepository;
import com.errorpurifier.domain.cache.repository.ErrorCacheRepository;
import com.errorpurifier.domain.cache.service.SensitiveDataSanitizer;
import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParsingAuditServiceTest {

    @Mock
    private ParsingAuditRepository auditRepository;
    @Mock
    private ClientDeviceRepository deviceRepository;
    @Mock
    private ErrorCacheRepository cacheRepository;
    @Mock
    private SensitiveDataSanitizer sensitiveDataSanitizer;
    @InjectMocks
    private ParsingAuditService auditService;

    @Test
    void storesOnlySanitizedAuditContent() {
        UUID deviceId = UUID.randomUUID();
        ClientDevice device = ClientDevice.builder().id(deviceId).pluginVersion("1.0.0").build();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
        when(sensitiveDataSanitizer.sanitize("password=secret")).thenReturn("password=[REDACTED]");
        when(sensitiveDataSanitizer.sanitize("parsed password=secret")).thenReturn("parsed password=[REDACTED]");

        auditService.record(deviceId.toString(), new ParsingAuditRequest(ParsingAuditLog.IssueType.PARSING_ERROR,
                "password=secret", "parsed password=secret", "incorrect extraction", null));

        ArgumentCaptor<ParsingAuditLog> captor = ArgumentCaptor.forClass(ParsingAuditLog.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getRawLogContent()).isEqualTo("password=[REDACTED]");
        assertThat(captor.getValue().getParsedLogContent()).isEqualTo("parsed password=[REDACTED]");
        assertThat(captor.getValue().isMasked()).isTrue();
    }
}
