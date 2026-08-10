package com.errorpurifier.domain.client.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Instance-local burst limiter plus the persistent per-day count on ClientDevice. */
@Component
@RequiredArgsConstructor
public class DeviceRequestLimiter {

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<UUID, ArrayDeque<Long>> recentRequests = new ConcurrentHashMap<>();

    public void verify(UUID deviceId, int dailyRequestCount) {
        if (dailyRequestCount >= properties.getDailyLimit()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "오늘의 정제 요청 한도(" + properties.getDailyLimit() + "회)를 초과했습니다. 내일 다시 시도하세요.");
        }

        long now = System.currentTimeMillis();
        long windowStart = now - properties.getBurstWindowSeconds() * 1_000L;
        ArrayDeque<Long> timestamps = recentRequests.computeIfAbsent(deviceId, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= properties.getBurstLimit()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        properties.getBurstWindowSeconds() + "초 안에 요청이 너무 많습니다. 잠시 후 다시 시도하세요.");
            }
            timestamps.addLast(now);
        }
    }
}
