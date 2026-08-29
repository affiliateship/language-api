package com.languageui.api.config;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;

class LocalDatabaseResetConfigTest {

    @Test
    void cleansBeforeMigrating() {
        Flyway flyway = mock(Flyway.class);
        FlywayMigrationStrategy strategy = new LocalDatabaseResetConfig().cleanAndMigrateFlyway();

        strategy.migrate(flyway);

        InOrder calls = inOrder(flyway);
        calls.verify(flyway).clean();
        calls.verify(flyway).migrate();
    }
}
