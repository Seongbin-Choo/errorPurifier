package com.errorpurifier.global.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void preservesExpectedStatusAndMessageForBusinessErrors() {
        var response = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .extracting(ApiErrorResponse::code, ApiErrorResponse::message, ApiErrorResponse::fieldErrors)
                .containsExactly("FORBIDDEN", "관리자 권한이 필요합니다.", java.util.Map.of());
    }
}
