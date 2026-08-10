package com.errorpurifier.domain.dashboard.controller;

import com.errorpurifier.domain.dashboard.dto.AdminDashboardResponse;
import com.errorpurifier.domain.dashboard.service.AdminDashboardService;
import com.errorpurifier.global.security.AdminAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminAccessService adminAccessService;
    private final AdminDashboardService dashboardService;

    @GetMapping
    public AdminDashboardResponse summary(@RequestHeader(value = "X-Admin-Token", required = false) String adminToken) {
        adminAccessService.requireAdmin(adminToken);
        return dashboardService.summary();
    }
}
