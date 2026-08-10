package com.errorpurifier.domain.audit.service;

import com.errorpurifier.domain.audit.dto.ParsingAuditRequest;
import com.errorpurifier.domain.audit.dto.ParsingAuditResponse;
import com.errorpurifier.domain.audit.entity.ParsingAuditLog;
import com.errorpurifier.domain.audit.repository.ParsingAuditRepository;
import com.errorpurifier.domain.cache.repository.ErrorCacheRepository;
import com.errorpurifier.domain.cache.service.SensitiveDataSanitizer;
import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.entity.DeviceStatus;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParsingAuditService {
    private final ParsingAuditRepository auditRepository;
    private final ClientDeviceRepository deviceRepository;
    private final ErrorCacheRepository cacheRepository;
    private final SensitiveDataSanitizer sensitiveDataSanitizer;

    @Transactional
    public void record(String deviceId, ParsingAuditRequest request) {
        ClientDevice device = getActiveDevice(deviceId);
        String sanitizedRawLog = sensitiveDataSanitizer.sanitize(request.rawLogContent());
        String sanitizedParsedLog = sensitiveDataSanitizer.sanitize(request.parsedLogContent());
        auditRepository.save(ParsingAuditLog.builder()
                .device(device)
                .linkedCache(request.cacheKey() == null ? null : cacheRepository.findByCacheKey(request.cacheKey()).orElse(null))
                .issueType(request.issueType())
                .rawLogContent(sanitizedRawLog)
                .parsedLogContent(sanitizedParsedLog)
                .userComment(request.userComment())
                .isMasked(!sanitizedRawLog.equals(request.rawLogContent())
                        || !java.util.Objects.equals(sanitizedParsedLog, request.parsedLogContent()))
                .build());
    }

    @Transactional(readOnly = true)
    public Page<ParsingAuditResponse> findAll(Pageable pageable) {
        return auditRepository.findAllByOrderByCreatedAtDescIdDesc(pageable).map(ParsingAuditResponse::from);
    }

    @Transactional
    public void markReviewed(Long auditId) {
        auditRepository.findById(auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "감사 로그를 찾을 수 없습니다."))
                .markAsReviewed();
    }

    private ClientDevice getActiveDevice(String deviceId) {
        try {
            ClientDevice device = deviceRepository.findById(UUID.fromString(deviceId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "등록되지 않은 디바이스입니다."));
            if (device.getStatus() != DeviceStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "현재 디바이스는 요청을 수행할 수 없습니다.");
            }
            return device;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Device-UUID 형식이 올바르지 않습니다.");
        }
    }
}
