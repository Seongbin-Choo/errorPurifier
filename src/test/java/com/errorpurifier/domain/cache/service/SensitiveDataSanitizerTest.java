package com.errorpurifier.domain.cache.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataSanitizerTest {

    private final SensitiveDataSanitizer sanitizer = new SensitiveDataSanitizer();

    @Test
    void masksCredentialsButKeepsDiagnosticContext() {
        String sanitized = sanitizer.sanitize("""
                java.sql.SQLException: access denied password=local-secret
                Authorization: Bearer token-value-1234567890
                jdbc:mariadb://db/app?user=alice&password=db-secret
                apiKey: AIza123456789012345678901234567890
                """);

        assertThat(sanitized).contains("java.sql.SQLException", "password=[REDACTED]", "Authorization: Bearer [REDACTED]");
        assertThat(sanitized).doesNotContain("local-secret", "token-value-1234567890", "db-secret", "AIza123456789012345678901234567890");
    }

    @Test
    void masksPrivateKeyBlock() {
        String sanitized = sanitizer.sanitize("""
                -----BEGIN PRIVATE KEY-----
                fake-private-material
                -----END PRIVATE KEY-----
                java.lang.IllegalStateException: failed
                """);

        assertThat(sanitized).contains("[REDACTED]", "java.lang.IllegalStateException");
        assertThat(sanitized).doesNotContain("fake-private-material");
    }
}
