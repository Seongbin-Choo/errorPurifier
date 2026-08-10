package com.errorpurifier.domain.feedback.service;

import com.errorpurifier.domain.cache.repository.ErrorCacheRepository;
import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.entity.DeviceStatus;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import com.errorpurifier.domain.feedback.dto.RefinementFeedbackRequest;
import com.errorpurifier.domain.feedback.entity.RefinementFeedback;
import com.errorpurifier.domain.feedback.repository.RefinementFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefinementFeedbackService {
    private final RefinementFeedbackRepository feedbackRepository;
    private final ClientDeviceRepository deviceRepository;
    private final ErrorCacheRepository cacheRepository;

    @Transactional
    public void record(String deviceId, RefinementFeedbackRequest request) {
        ClientDevice device = getDevice(deviceId);
        feedbackRepository.save(RefinementFeedback.builder()
                .device(device)
                .cache(cacheRepository.findByCacheKey(request.cacheKey()).orElse(null))
                .feedbackType(request.feedbackType())
                .originalCharacters(request.originalCharacters())
                .preparedCharacters(request.preparedCharacters())
                .appliedRuleCounts(request.appliedRuleCounts())
                .protectedLineCount(request.protectedLineCount())
                .logTruncated(request.logTruncated())
                .build());
    }

    private ClientDevice getDevice(String deviceId) {
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
