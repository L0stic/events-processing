package com.rivada.events.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivada.events.config.model.SqsQueueConfig;
import com.rivada.events.service.model.sqs.SqsMessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.UUID;

@Component
@Slf4j
public class AwsSQSPublisherConnector {

    private final ObjectMapper objectMapper;
    private final SqsAsyncClient sqsClient;
    private String chainEventQueueUrl;
    private final Boolean eventQueueEnabled;

    public AwsSQSPublisherConnector(SqsQueueConfig eventQueueConfig,
                                    ObjectMapper objectMapper,
                                    @Value("${config.aws.sqs.enabled}") Boolean eventQueueEnabled) {
        sqsClient = SqsAsyncClient.builder()
                .region(Region.of(eventQueueConfig.getRegion()))
                .build();
        this.eventQueueEnabled = eventQueueEnabled;
        if (this.eventQueueEnabled) {
            this.loadQueueUrlForTopicName(eventQueueConfig.getQueueName())
                    .subscribe(queueUrl -> {
                        log.info("Topic details was loaded successfully for queue: {}. Url: {}", eventQueueConfig.getQueueName(), queueUrl);
                        chainEventQueueUrl = queueUrl;
                    });
        } else {
            log.warn("AwsSQSPublisherConnector was disabled in settings. Check config!");
        }
        this.objectMapper = objectMapper;
    }

    private void disposeComponent() {
        sqsClient.close();
    }

    public Mono<SendMessageResponse> sendMessage(SqsMessageRequest<?> messageRequest) {
        return this.eventQueueEnabled ? sendSQSMessage(messageRequest) : emulateSendMessage(messageRequest);
    }

    private Mono<SendMessageResponse> sendSQSMessage(SqsMessageRequest<?> messageRequest) {
        // Create a SendMessageRequest
        SendMessageRequest sendMessageRequest = null;
        try {
            sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(chainEventQueueUrl)
                    .messageBody(objectMapper.writeValueAsString(messageRequest.getMessage()))
                    .build();
        } catch (Exception e) {
            if (e instanceof JsonProcessingException) {
                log.error("Can't send message because of conversion to json string. Full data: {}", messageRequest);
            }else{
                log.error("Can't send SQS message: {}. Original error message: {}", messageRequest, e.getMessage());
            }
        }
        // Create a Mono that sends the message and emits the response
        return Mono.fromCompletionStage(sqsClient.sendMessage(sendMessageRequest));
    }

    private Mono<SendMessageResponse> emulateSendMessage(SqsMessageRequest<?> messageRequest) {
        // Create a SendMessageRequest
        var response = SendMessageResponse.builder()
                .messageId(UUID.randomUUID().toString())
                .build();
        // Create a Mono that sends the message and emits the response
        return Mono.just(response);
    }

    private Mono<String> loadQueueUrlForTopicName(String queueName) {
        try {
            log.info("Try to load topic url for queue: {}", queueName);
            ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder()
                    .queueNamePrefix(queueName)
                    .build();

            return Mono.fromCompletionStage(sqsClient.listQueues(listQueuesRequest))
                    .mapNotNull(response -> {
                        if (response.queueUrls().isEmpty()) {
                            log.error("There is no topic in SQS AWS for queue: '{}'", queueName);
                            return null;
                        }
                        return response.queueUrls().get(0);
                    });
        } catch (SqsException e) {
            log.error(e.awsErrorDetails().errorMessage());
        }
        return Mono.empty();
    }
}
