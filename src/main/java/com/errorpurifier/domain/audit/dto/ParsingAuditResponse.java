package com.errorpurifier.domain.audit.dto;

import com.errorpurifier.domain.audit.entity.ParsingAuditLog;

import java.time.LocalDateTime;

public record ParsingAuditResponse(Long id, String deviceId, Long cacheId, String issueType, String rawLogContent,
                                   String parsedLogContent, String userComment, boolean masked, boolean reviewed,
                                   LocalDateTime createdAt) {
    public static ParsingAuditResponse from(ParsingAuditLog audit) {
        return new ParsingAuditResponse(audit.getId(), audit.getDevice().getId().toString(),
                audit.getLinkedCache() == null ? null : audit.getLinkedCache().getId(), audit.getIssueType().name(),
                audit.getRawLogContent(), audit.getParsedLogContent(), audit.getUserComment(), audit.isMasked(),
                audit.isReviewed(), audit.getCreatedAt());
    }
}
