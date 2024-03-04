package com.rivada.events.config;

import com.rivada.events.config.model.DbConfig;
import com.rivada.events.service.AwsSecretsService;
import lombok.RequiredArgsConstructor;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FlywayConfiguration {

    final static String FLYWAY_JDBC_URL_PREFIX = "jdbc";

    final protected AwsSecretsService awsSecretsService;

    final DbConfig dbConfig;

    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        String flywayUrl = FLYWAY_JDBC_URL_PREFIX + ":" + dbConfig.getDriver() + "://" + dbConfig.getHost() + ":" + dbConfig.getPort() + "/" + dbConfig.getDatabase();
        var flyway = new Flyway(Flyway.configure()
                .baselineOnMigrate(true)
                .dataSource(
                        flywayUrl,
                        dbConfig.getUsername(),
                        dbConfig.getPassword())
        );
        flyway.repair();
        flyway.migrate();
        return flyway;
    }
}
