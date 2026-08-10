package com.errorpurifier.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class AdminAccessService {

    private static final String INSECURE_PLACEHOLDER = "change-me";

    private final String adminToken;

    public AdminAccessService(@Value("${error-purifier.admin.token:}") String adminToken) {
        this.adminToken = adminToken == null ? "" : adminToken.trim();
    }

    public void requireAdmin(String providedToken) {
        if (adminToken.isBlank() || INSECURE_PLACEHOLDER.equalsIgnoreCase(adminToken) || providedToken == null
                || !MessageDigest.isEqual(adminToken.getBytes(StandardCharsets.UTF_8), providedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다.");
        }
    }
}
