package com.errorpurifier;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorPurifierApplicationTimezoneTest {

    @Test
    void configuresJvmDefaultTimezoneAsUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        ErrorPurifierApplication.configureUtcTimezone();

        assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
        assertThat(ZoneId.systemDefault().normalized()).isEqualTo(ZoneOffset.UTC);
    }
}
