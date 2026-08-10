package com.errorpurifier.domain.client.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record InitRequest(
        String deviceUuid,

        @NotBlank(message = "플러그인 버전은 필수입니다.")
        String pluginVersion
) {}