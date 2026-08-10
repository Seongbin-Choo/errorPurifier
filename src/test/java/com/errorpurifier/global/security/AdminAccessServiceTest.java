package com.errorpurifier.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminAccessServiceTest {

    @Test
    void allowsOnlyConfiguredToken() {
        AdminAccessService service = new AdminAccessService("admin-test-token");

        assertThatCode(() -> service.requireAdmin("admin-test-token")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.requireAdmin("wrong-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("관리자 권한");
    }

    @Test
    void deniesAccessWhenTokenIsNotConfigured() {
        assertThatThrownBy(() -> new AdminAccessService("").requireAdmin("anything"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void deniesTheExampleTokenEvenWhenItIsProvided() {
        assertThatThrownBy(() -> new AdminAccessService("change-me").requireAdmin("change-me"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("관리자 권한");
    }
}
