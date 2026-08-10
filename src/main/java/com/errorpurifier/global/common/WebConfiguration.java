package com.errorpurifier.global.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebConfiguration {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(100);
            resolver.setFallbackPageable(org.springframework.data.domain.PageRequest.of(0, 20));
        };
    }
}
