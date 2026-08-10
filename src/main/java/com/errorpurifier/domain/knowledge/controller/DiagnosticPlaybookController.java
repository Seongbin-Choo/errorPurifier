package com.errorpurifier.domain.knowledge.controller;

import com.errorpurifier.domain.knowledge.dto.DiagnosticPlaybookRequest;
import com.errorpurifier.domain.knowledge.dto.DiagnosticPlaybookPreviewRequest;
import com.errorpurifier.domain.knowledge.dto.DiagnosticPlaybookPreviewResponse;
import com.errorpurifier.domain.knowledge.dto.DiagnosticPlaybookPatternPreviewRequest;
import com.errorpurifier.domain.knowledge.dto.DiagnosticPlaybookPatternPreviewResponse;
import com.errorpurifier.domain.knowledge.dto.DiagnosticPlaybookResponse;
import com.errorpurifier.domain.knowledge.service.DiagnosticPlaybookMatcher;
import com.errorpurifier.domain.knowledge.service.DiagnosticPlaybookService;
import com.errorpurifier.global.security.AdminAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/diagnostic-playbooks")
@RequiredArgsConstructor
public class DiagnosticPlaybookController {
    private final AdminAccessService adminAccessService;
    private final DiagnosticPlaybookService playbookService;
    private final DiagnosticPlaybookMatcher playbookMatcher;

    @GetMapping
    public List<DiagnosticPlaybookResponse> findAll(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        adminAccessService.requireAdmin(adminToken);
        return playbookService.findAll();
    }

    @PostMapping
    public ResponseEntity<DiagnosticPlaybookResponse> create(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                              @Valid @RequestBody DiagnosticPlaybookRequest request) {
        adminAccessService.requireAdmin(adminToken);
        DiagnosticPlaybookResponse response = playbookService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/diagnostic-playbooks/" + response.id())).body(response);
    }

    @PostMapping("/preview")
    public List<DiagnosticPlaybookPreviewResponse> preview(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                            @Valid @RequestBody DiagnosticPlaybookPreviewRequest request) {
        adminAccessService.requireAdmin(adminToken);
        return playbookMatcher.findMatches(request.log()).stream().map(DiagnosticPlaybookPreviewResponse::from).toList();
    }

    @PostMapping("/preview-pattern")
    public DiagnosticPlaybookPatternPreviewResponse previewPattern(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                                    @Valid @RequestBody DiagnosticPlaybookPatternPreviewRequest request) {
        adminAccessService.requireAdmin(adminToken);
        return new DiagnosticPlaybookPatternPreviewResponse(playbookService.matches(request.matchPattern(), request.log()));
    }

    @PutMapping("/{playbookId}")
    public DiagnosticPlaybookResponse update(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                             @PathVariable Long playbookId, @Valid @RequestBody DiagnosticPlaybookRequest request) {
        adminAccessService.requireAdmin(adminToken);
        return playbookService.update(playbookId, request);
    }

    @PatchMapping("/{playbookId}/active")
    public ResponseEntity<Void> setActive(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                          @PathVariable Long playbookId, @RequestBody ActiveRequest request) {
        adminAccessService.requireAdmin(adminToken);
        playbookService.setActive(playbookId, request.active());
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {
    }
}
