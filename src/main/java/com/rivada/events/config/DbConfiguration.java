package com.rivada.events.config;

import com.rivada.events.config.model.DbConfig;
import com.rivada.events.service.AwsSecretsService;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@Slf4j
@EnableR2dbcRepositories
@EnableR2dbcAuditing
@RequiredArgsConstructor
public class DbConfiguration extends AbstractR2dbcConfiguration {
    @Value("${config.secrets.db}")
    private String dbSecret;

    final AwsSecretsService awsSecretsService;

    final Environment env;

    @Bean
    public DbConfig dbConfig() {
        if(env.matchesProfiles("dev", "local")) {
            return DbConfig.builder()
                    .host(env.getProperty("config.database.host"))
                    .port(env.getProperty("config.database.port", Integer.class))
                    .database(env.getProperty("config.database.database"))
                    .driver(env.getProperty("config.database.driver"))
                    .username(env.getProperty("config.database.username"))
                    .password(env.getProperty("config.database.password"))
                    .build();
        } else {
            return awsSecretsService.getSecret(dbSecret, DbConfig.class);
        }
    }

    @NotNull
    @Override
    @Bean
    public ConnectionFactory connectionFactory() {
        DbConfig dbConfig = dbConfig();
        var options = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.HOST, dbConfig.getHost())
                .option(ConnectionFactoryOptions.PORT, dbConfig.getPort())
                .option(ConnectionFactoryOptions.DATABASE, dbConfig.getDatabase())
                .option(ConnectionFactoryOptions.DRIVER, dbConfig.getDriver())
                .option(ConnectionFactoryOptions.USER, dbConfig.getUsername())
                .option(ConnectionFactoryOptions.PASSWORD, dbConfig.getPassword())
                .build();
        return ConnectionFactories.get(options);
    }
}

