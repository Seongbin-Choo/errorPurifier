package com.errorpurifier.global.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception) {
        int status = exception.getStatusCode().value();
        return response(status, exception.getReason() == null ? "요청을 처리할 수 없습니다." : exception.getReason(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST.value(), "요청 값이 올바르지 않습니다.", fieldErrors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST.value(), "요청 본문 또는 필수 헤더 형식이 올바르지 않습니다.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("처리되지 않은 요청 오류: {} {}", request.getMethod(), request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다.", Map.of());
    }

    private ResponseEntity<ApiErrorResponse> response(int status, String message, Map<String, String> fieldErrors) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(Instant.now(), status,
                HttpStatus.valueOf(status).name(), message, fieldErrors));
    }
}
