package com.errorpurifier.domain.feedback.controller;

import com.errorpurifier.domain.feedback.dto.RefinementFeedbackRequest;
import com.errorpurifier.domain.feedback.service.RefinementFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/refinement-feedback")
@RequiredArgsConstructor
public class RefinementFeedbackController {
    private final RefinementFeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<Void> record(@RequestHeader("X-Device-UUID") String deviceId,
                                       @Valid @RequestBody RefinementFeedbackRequest request) {
        feedbackService.record(deviceId, request);
        return ResponseEntity.noContent().build();
    }
}
