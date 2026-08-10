package com.errorpurifier.domain.client.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceRequestLimiterTest {

    @Test
    void rejectsRequestWhenDailyLimitIsReached() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setDailyLimit(2);
        DeviceRequestLimiter limiter = new DeviceRequestLimiter(properties);

        assertThatThrownBy(() -> limiter.verify(UUID.randomUUID(), 2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("오늘의 정제 요청 한도");
    }

    @Test
    void rejectsBurstRequests() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setBurstLimit(2);
        DeviceRequestLimiter limiter = new DeviceRequestLimiter(properties);
        UUID deviceId = UUID.randomUUID();

        limiter.verify(deviceId, 0);
        limiter.verify(deviceId, 1);

        assertThatThrownBy(() -> limiter.verify(deviceId, 2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("요청이 너무 많습니다");
    }
}
