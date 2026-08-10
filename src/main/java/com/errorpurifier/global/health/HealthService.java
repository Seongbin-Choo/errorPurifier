package com.errorpurifier.global.health;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class HealthService {
    private final DataSource dataSource;

    public boolean isReady() {
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (SQLException exception) {
            return false;
        }
    }
}
