package com.rivada.events.config;

import com.rivada.events.config.model.SqsQueueConfig;
import com.rivada.events.service.AwsSecretsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
public class QueuesConfiguration {

    @Value("${config.secrets.events-queue}")
    private String eventsQueueConfigSecret;
    final AwsSecretsService awsSecretsService;
    final Environment env;

    @Bean
    public SqsQueueConfig eventsQueueConfig() {
        if(env.matchesProfiles("dev", "local")) {
            return SqsQueueConfig.builder()
                    .region(env.getProperty("config.events.region"))
                    .queueName(env.getProperty("config.events.eventQueueName"))
                    .build();
        } else {
            return awsSecretsService.getSecret(eventsQueueConfigSecret, SqsQueueConfig.class);
        }
    }
}
