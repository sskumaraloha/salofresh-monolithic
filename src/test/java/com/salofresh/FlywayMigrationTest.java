package com.salofresh;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

    private static final String JDBC_URL =
            "jdbc:h2:mem:flyway_verify;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE";

    @Test
    void allMigrationsApplyCleanlyToAFreshSchema() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(JDBC_URL, "sa", "")
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(2);

        try (Connection connection = DriverManager.getConnection(JDBC_URL, "sa", "");
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("select count(*) from users")) {
                rs.next();
                assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(1);
            }
            try (ResultSet rs = statement.executeQuery("select count(*) from roles")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(5);
            }
            try (ResultSet rs = statement.executeQuery("select count(*) from categories")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(18);
            }
        }
    }
}
