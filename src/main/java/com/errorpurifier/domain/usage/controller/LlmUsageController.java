package com.errorpurifier.domain.usage.controller;

import com.errorpurifier.domain.usage.dto.LlmUsageRequest;
import com.errorpurifier.domain.usage.dto.LlmUsageResponse;
import com.errorpurifier.domain.usage.dto.LlmFeedbackRequest;
import com.errorpurifier.domain.usage.dto.LlmUsageSummaryResponse;
import com.errorpurifier.domain.usage.service.LlmUsageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class LlmUsageController {
    private final LlmUsageService llmUsageService;

    @PostMapping
    public ResponseEntity<LlmUsageResponse> record(@RequestHeader("X-Device-UUID") String deviceId,
                                                    @Valid @RequestBody LlmUsageRequest request) {
        return ResponseEntity.status(201).body(llmUsageService.record(deviceId, request));
    }

    @PatchMapping("/{usageId}/feedback")
    public ResponseEntity<Void> feedback(@RequestHeader("X-Device-UUID") String deviceId,
                                         @PathVariable Long usageId,
                                         @Valid @RequestBody LlmFeedbackRequest request) {
        llmUsageService.recordFeedback(deviceId, usageId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary")
    public LlmUsageSummaryResponse summary(@RequestHeader("X-Device-UUID") String deviceId) {
        return llmUsageService.summary(deviceId);
    }
}
