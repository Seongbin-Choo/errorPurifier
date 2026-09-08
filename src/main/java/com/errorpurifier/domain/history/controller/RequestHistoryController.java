package com.errorpurifier.domain.history.controller;

import com.errorpurifier.domain.history.dto.RequestHistoryResponse;
import com.errorpurifier.domain.history.service.RequestHistoryService;
import com.errorpurifier.global.common.CursorSlice;
import com.errorpurifier.global.security.AdminAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class RequestHistoryController {
    private final RequestHistoryService historyService;
    private final AdminAccessService adminAccessService;

    @GetMapping
    public CursorSlice<RequestHistoryResponse> findAll(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        adminAccessService.requireAdmin(adminToken);
        return historyService.findSlice(cursor, size);
    }
}
