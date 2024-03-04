package com.rivada.events.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
@RequiredArgsConstructor
public class AwsConfiguration {
    @Bean
    public SecretsManagerClient secretsManagerClient(@Value("${config.aws.region}") String region) {
        return SecretsManagerClient.builder()
                .region(Region.of(region))
                .build();
    }
}
