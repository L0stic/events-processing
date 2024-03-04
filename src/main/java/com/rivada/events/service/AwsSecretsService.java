package com.rivada.events.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

@Slf4j
@RequiredArgsConstructor
@Service
public class AwsSecretsService {
    final SecretsManagerClient secretsManagerClient;

    public <T> T getSecret(String secretName, Class<T> valueType) {
        try {
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();
            String secret = secretsManagerClient.getSecretValue(getSecretValueRequest).secretString();
            return new ObjectMapper().readValue(secret, valueType);
        } catch (JsonProcessingException e) {
            log.error("Unable to retrieve or parse secret: " + secretName);
            throw new RuntimeException(e);
        }
    }
}
