package com.errorpurifier.domain.client.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "error-purifier.rate-limit")
public class RateLimitProperties {
    private int dailyLimit = 100;
    private int burstLimit = 10;
    private int burstWindowSeconds = 60;
}
