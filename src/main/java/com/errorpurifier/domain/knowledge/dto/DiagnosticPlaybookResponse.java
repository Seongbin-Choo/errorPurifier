package com.errorpurifier.domain.knowledge.dto;

import com.errorpurifier.domain.knowledge.entity.DiagnosticPlaybook;

public record DiagnosticPlaybookResponse(Long id, String name, String matchPattern, String guidance, int priority,
                                         boolean active, long matchCount) {
    public static DiagnosticPlaybookResponse from(DiagnosticPlaybook playbook) {
        return new DiagnosticPlaybookResponse(playbook.getId(), playbook.getName(), playbook.getMatchPattern(),
                playbook.getGuidance(), playbook.getPriority(), playbook.isActive(), playbook.getMatchCount());
    }
}
