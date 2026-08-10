package com.errorpurifier.domain.rule.controller;

import com.errorpurifier.domain.rule.dto.LogParsingRuleRequest;
import com.errorpurifier.domain.rule.dto.LogParsingRuleResponse;
import com.errorpurifier.domain.rule.service.LogParsingRuleService;
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
@RequestMapping("/api/v1/rule")
@RequiredArgsConstructor
public class LogParsingRuleController {
    private final AdminAccessService adminAccessService;
    private final LogParsingRuleService ruleService;

    @GetMapping
    public List<LogParsingRuleResponse> findAll(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        adminAccessService.requireAdmin(adminToken);
        return ruleService.findAll();
    }

    @PostMapping
    public ResponseEntity<LogParsingRuleResponse> create(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
                                                         @Valid @RequestBody LogParsingRuleRequest request) {
        adminAccessService.requireAdmin(adminToken);
        LogParsingRuleResponse response = ruleService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/rule/" + response.id())).body(response);
    }

    @PutMapping("/{ruleId}")
    public LogParsingRuleResponse update(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken, @PathVariable Long ruleId,
                                         @Valid @RequestBody LogParsingRuleRequest request) {
        adminAccessService.requireAdmin(adminToken);
        return ruleService.update(ruleId, request);
    }

    @PatchMapping("/{ruleId}/active")
    public ResponseEntity<Void> setActive(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken, @PathVariable Long ruleId,
                                          @RequestBody ActiveRequest request) {
        adminAccessService.requireAdmin(adminToken);
        ruleService.setActive(ruleId, request.active());
        return ResponseEntity.noContent().build();
    }

    public record ActiveRequest(boolean active) {
    }
}
