package com.errorpurifier.global.health;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthServiceTest {

    @Test
    void reportsReadyWhenDatabaseConnectionIsValid() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        assertThat(new HealthService(dataSource).isReady()).isTrue();
    }

    @Test
    void reportsNotReadyWhenDatabaseConnectionCannotBeOpened() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("database unavailable"));

        assertThat(new HealthService(dataSource).isReady()).isFalse();
    }
}
