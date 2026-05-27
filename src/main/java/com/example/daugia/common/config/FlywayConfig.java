package com.example.daugia.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class FlywayConfig {

    @Bean
    public FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
        // Return a no-op initializer so Spring Boot doesn't run it before Hibernate
        return new FlywayMigrationInitializer(flyway, f -> {});
    }

    @Bean
    public ApplicationRunner flywayRunner(Flyway flyway) {
        return args -> {
            flyway.migrate();
        };
    }
}
