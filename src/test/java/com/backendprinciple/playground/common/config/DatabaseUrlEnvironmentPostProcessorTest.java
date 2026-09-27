package com.backendprinciple.playground.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsPlatformUrlToJdbcProperties() {
        var props = DatabaseUrlEnvironmentPostProcessor.toSpringProperties(
                "postgresql://app_user:p%40ss:word@db.example.com:5432/playground?sslmode=require");
        assertThat(props)
                .containsEntry("spring.datasource.url", "jdbc:postgresql://db.example.com:5432/playground?sslmode=require")
                .containsEntry("spring.datasource.username", "app_user")
                .containsEntry("spring.datasource.password", "p@ss:word");
    }

    @Test
    void acceptsNeonConnectionStrings() {
        var props = DatabaseUrlEnvironmentPostProcessor.toSpringProperties(
                "postgresql://neondb_owner:npg_Ab12@ep-cool-sun-a1b2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require");
        assertThat(props)
                .containsEntry("spring.datasource.url",
                        "jdbc:postgresql://ep-cool-sun-a1b2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require")
                .containsEntry("spring.datasource.username", "neondb_owner")
                .containsEntry("spring.datasource.password", "npg_Ab12");
    }

    @Test
    void worksWithoutPortOrQuery() {
        var props = DatabaseUrlEnvironmentPostProcessor.toSpringProperties("postgres://u:p@host/db");
        assertThat(props).containsEntry("spring.datasource.url", "jdbc:postgresql://host/db");
    }

    @Test
    void rejectsOtherDatabases() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor.toSpringProperties("mysql://u:p@h/db"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
