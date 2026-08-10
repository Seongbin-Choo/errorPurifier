package com.errorpurifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class ErrorPurifierApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErrorPurifierApplication.class, args);
    }

}
