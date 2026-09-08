package com.errorpurifier.domain.history.dto;

import com.errorpurifier.domain.history.entity.RequestHistory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public record RequestHistoryCursor(LocalDateTime createdAt, Long id) {

    private static final String DELIMITER = "_";

    public static RequestHistoryCursor from(RequestHistory history) {
        return new RequestHistoryCursor(history.getCreatedAt(), history.getId());
    }

    public static RequestHistoryCursor decode(String encoded) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            int separator = decoded.lastIndexOf(DELIMITER);
            return new RequestHistoryCursor(LocalDateTime.parse(decoded.substring(0, separator)),
                    Long.parseLong(decoded.substring(separator + 1)));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cursor 값이 올바르지 않습니다.");
        }
    }

    public String encode() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((createdAt + DELIMITER + id).getBytes(StandardCharsets.UTF_8));
    }
}
