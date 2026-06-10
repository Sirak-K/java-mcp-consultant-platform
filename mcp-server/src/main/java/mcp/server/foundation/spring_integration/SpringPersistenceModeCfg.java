package mcp.server.foundation.spring_integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * Mode-driven DataSource wiring.
 *
 * <p>Active only when {@code mcp.customer-data.mode=in-memory}.
 * Provides an H2 in-memory {@link DataSource}, suppressing Spring Boot's
 * HikariCP auto-configuration (which is conditional on missing bean).
 *
 * <p>For DB-backed mode ({@code postgresql}),
 * this configuration is inactive and Spring Boot auto-configures HikariCP
 * from {@code spring.datasource.*} properties as normal.
 */
@Configuration
@ConditionalOnProperty(name = "mcp.customer-data.mode", havingValue = "in-memory")
public class SpringPersistenceModeCfg {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("db_mcp")
                .build();
    }
}
