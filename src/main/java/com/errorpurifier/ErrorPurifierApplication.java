package com.errorpurifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class ErrorPurifierApplication {

    private static final String APPLICATION_TIME_ZONE = "UTC";

    static {
        configureUtcTimezone();
    }

    public static void main(String[] args) {
        SpringApplication.run(ErrorPurifierApplication.class, args);
    }

    static void configureUtcTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_TIME_ZONE));
    }

}
