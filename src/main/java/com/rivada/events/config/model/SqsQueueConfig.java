package com.rivada.events.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class SqsQueueConfig {
    private String region;

    @JsonProperty("queue_name")
    private String queueName;
}
