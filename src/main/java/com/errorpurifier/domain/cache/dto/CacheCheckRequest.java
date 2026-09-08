package com.errorpurifier.domain.cache.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
@Builder
public record CacheCheckRequest(
        @NotBlank(message = "콘솔 로그는 필수입니다.")
        @Size(max = CacheCheckRequest.MAX_LOG_PAYLOAD_CHARACTERS,
                message = "콘솔 로그는 1,000,000자를 넘을 수 없습니다.")
        String rawLog,

        @Size(max = CacheCheckRequest.MAX_LOG_PAYLOAD_CHARACTERS,
                message = "선택 로그는 1,000,000자를 넘을 수 없습니다.")
        String selectedText,

        @NotNull(message = "프로젝트 파일 정보는 필수입니다.")
        Map<String, String> projectFiles,

        @NotNull(message = "환경 정보 태그는 필수입니다.")
        Map<String, String> environmentTags
) {
    public static final int MAX_LOG_PAYLOAD_CHARACTERS = 1_000_000;
}
