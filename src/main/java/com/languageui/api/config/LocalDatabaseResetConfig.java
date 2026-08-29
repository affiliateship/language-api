package com.languageui.api.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class LocalDatabaseResetConfig {

    @Bean
    FlywayMigrationStrategy cleanAndMigrateFlyway() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}
