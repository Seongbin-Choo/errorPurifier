package com.errorpurifier.domain.audit.controller;

import com.errorpurifier.domain.audit.dto.ParsingAuditRequest;
import com.errorpurifier.domain.audit.dto.ParsingAuditResponse;
import com.errorpurifier.domain.audit.service.ParsingAuditService;
import com.errorpurifier.global.common.CursorSlice;
import com.errorpurifier.global.security.AdminAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class ParsingAuditController {
    private final ParsingAuditService auditService;
    private final AdminAccessService adminAccessService;

    @PostMapping
    public ResponseEntity<Void> record(@RequestHeader("X-Device-UUID") String deviceId,
                                       @Valid @RequestBody ParsingAuditRequest request) {
        auditService.record(deviceId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public CursorSlice<ParsingAuditResponse> findAll(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        adminAccessService.requireAdmin(adminToken);
        return auditService.findSlice(cursor, size);
    }

    @PatchMapping("/{auditId}/reviewed")
    public ResponseEntity<Void> markReviewed(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                             @PathVariable Long auditId) {
        adminAccessService.requireAdmin(adminToken);
        auditService.markReviewed(auditId);
        return ResponseEntity.noContent().build();
    }
}
