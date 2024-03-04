package com.rivada.events.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rivada.events.config.model.DefragmentationConfig;
import com.rivada.events.service.AwsSecretsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.math.BigInteger;

@Configuration
@RequiredArgsConstructor
public class DefragmentationConfiguration {

    @Value("${config.secrets.defragmentation}")
    private String eventsQueueConfigSecret;

    final AwsSecretsService awsSecretsService;
    final Environment env;

    @Bean
    public DefragmentationConfig eventsDefragmentationConfig() throws JsonProcessingException {
        if (env.matchesProfiles("dev", "local")) {
            return DefragmentationConfig.builder()
                    .blocksCountPerSecondEstimate(new BigInteger(env.getProperty("config.eventsDefragmentation.blocksCountPerSecondEstimate")))
                    .firsTask(DefragmentationConfig.DefragmentationTaskConfig.builder()
                            .delayMin(Long.valueOf(env.getProperty("config.eventsDefragmentation.first.delayMin")))
                            .timeoutMin(Long.valueOf(env.getProperty("config.eventsDefragmentation.first.timeoutMin")))
                            .eventsAgeScopeHours(Integer.valueOf(env.getProperty("config.eventsDefragmentation.first.eventsAgeScopeHours")))
                            .build())
                    .secondTask(DefragmentationConfig.DefragmentationTaskConfig.builder()
                            .delayMin(Long.valueOf(env.getProperty("config.eventsDefragmentation.second.delayMin")))
                            .timeoutMin(Long.valueOf(env.getProperty("config.eventsDefragmentation.second.timeoutMin")))
                            .eventsAgeScopeHours(Integer.valueOf(env.getProperty("config.eventsDefragmentation.second.eventsAgeScopeHours")))
                            .build())
                    .build();
        } else {
            return awsSecretsService.getSecret(eventsQueueConfigSecret, DefragmentationConfig.class);
        }
    }
}
