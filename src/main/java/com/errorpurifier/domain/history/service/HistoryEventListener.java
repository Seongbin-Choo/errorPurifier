package com.errorpurifier.domain.history.service;

import com.errorpurifier.domain.cache.entity.ErrorCache;
import com.errorpurifier.domain.cache.repository.ErrorCacheRepository;
import com.errorpurifier.domain.client.entity.ClientDevice;
import com.errorpurifier.domain.client.repository.ClientDeviceRepository;
import com.errorpurifier.domain.history.dto.HistoryEvent;
import com.errorpurifier.domain.history.entity.RequestHistory;
import com.errorpurifier.domain.history.entity.RequestHistory.RequestType;
import com.errorpurifier.domain.history.repository.RequestHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryEventListener {

    private final RequestHistoryRepository historyRepository;
    private final ClientDeviceRepository deviceRepository;
    private final ErrorCacheRepository cacheRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordHistory(HistoryEvent event) {
        try {
            ClientDevice device = deviceRepository.findById(UUID.fromString(event.deviceId()))
                    .orElseThrow(() -> new IllegalArgumentException("..."));

            ErrorCache cache = null;
            if (event.cacheId() != null) {
                cache = cacheRepository.findById(event.cacheId()).orElse(null);
            }

            RequestHistory history = RequestHistory.builder()
                    .device(device)
                    .cache(cache)
                    .requestType(RequestType.valueOf(event.requestType()))
                    .processingTimeMs((int) event.processingTimeMs())
                    .build();

            historyRepository.save(history);

        } catch (Exception e) {
            log.error("비동기 이력 저장 중 에러 발생: {}", e.getMessage());
        }
    }
}
