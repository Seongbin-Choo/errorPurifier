package com.errorpurifier.domain.client.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Limits only backend prompt-preparation calls; provider API calls stay on the user's machine. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "error-purifier.rate-limit")
public class RateLimitProperties {
    private int dailyLimit = 100;
    private int burstLimit = 10;
    private int burstWindowSeconds = 60;
}
