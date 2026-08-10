package com.errorpurifier.domain.feedback.controller;

import com.errorpurifier.domain.feedback.dto.RefinementQualitySummaryResponse;
import com.errorpurifier.domain.feedback.service.RefinementQualityReportService;
import com.errorpurifier.global.security.AdminAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/refinement-quality")
@RequiredArgsConstructor
public class RefinementQualityReportController {
    private final AdminAccessService adminAccessService;
    private final RefinementQualityReportService reportService;

    @GetMapping
    public RefinementQualitySummaryResponse summary(@RequestHeader("X-Admin-Token") String adminToken) {
        adminAccessService.requireAdmin(adminToken);
        return reportService.summary();
    }
}
