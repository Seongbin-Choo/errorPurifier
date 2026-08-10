package com.errorpurifier.domain.cache.controller;

import com.errorpurifier.domain.cache.dto.CacheCheckRequest;
import com.errorpurifier.domain.cache.dto.CacheCheckResponse;
import com.errorpurifier.domain.cache.dto.CacheContributeRequest;
import com.errorpurifier.domain.cache.service.ErrorCacheService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/prompt")
@RequiredArgsConstructor
public class ErrorCacheController {

    private final ErrorCacheService errorCacheService;

    @PostMapping("/prepare")
    public ResponseEntity<CacheCheckResponse> preparePrompt(
            @RequestHeader("X-Device-UUID") String deviceId,
            @Valid @RequestBody CacheCheckRequest request) {

        CacheCheckResponse response = errorCacheService.preparePrompt(request, deviceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/processes")
    public ResponseEntity<Void> contributeCache(
            @RequestHeader("X-Device-UUID") String deviceId,
            @Valid @RequestBody CacheContributeRequest request) {

        errorCacheService.contributeProcess(request, deviceId);
        return ResponseEntity.noContent().build();
    }
}
