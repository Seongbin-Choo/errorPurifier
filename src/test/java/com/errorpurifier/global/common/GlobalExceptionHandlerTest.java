package com.errorpurifier.global.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.resource.NoResourceFoundException;
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

    @Test
    void returnsNotFoundForAnUnknownPath() {
        var response = handler.handleNoResourceFound(new NoResourceFoundException(HttpMethod.GET, "", "/missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .extracting(ApiErrorResponse::code, ApiErrorResponse::message)
                .containsExactly("NOT_FOUND", "요청한 경로를 찾을 수 없습니다.");
    }
}
