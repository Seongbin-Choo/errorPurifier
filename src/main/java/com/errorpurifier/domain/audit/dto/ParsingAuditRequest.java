package com.errorpurifier.domain.audit.dto;

import com.errorpurifier.domain.audit.entity.ParsingAuditLog.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParsingAuditRequest(
        @NotNull IssueType issueType,
        @NotBlank @Size(max = 100_000) String rawLogContent,
        @Size(max = 100_000) String parsedLogContent,
        @Size(max = 500) String userComment,
        @Size(min = 64, max = 64) String cacheKey
) {
}
