package com.errorpurifier.domain.cache.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CacheContributeRequest {
    @NotBlank
    private String cacheKey;

    @NotBlank
    @Size(max = 20_000)
    private String processTemplate;

    @PositiveOrZero
    private int savedTokens;

}
