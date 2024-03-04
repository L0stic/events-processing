package com.rivada.events.service.model.sqs;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@ToString
@Getter
@Builder
public class SqsMessageRequest<T extends Serializable> {
     String id;
     String group;
     T message;
}
