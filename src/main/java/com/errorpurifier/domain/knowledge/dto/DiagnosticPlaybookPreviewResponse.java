package com.errorpurifier.domain.knowledge.dto;

import com.errorpurifier.domain.knowledge.service.DiagnosticPlaybookMatcher;

public record DiagnosticPlaybookPreviewResponse(String name, String guidance) {
    public static DiagnosticPlaybookPreviewResponse from(DiagnosticPlaybookMatcher.DiagnosticPlaybookMatch match) {
        return new DiagnosticPlaybookPreviewResponse(match.name(), match.guidance());
    }
}
