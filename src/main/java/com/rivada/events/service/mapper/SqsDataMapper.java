package com.rivada.events.service.mapper;

import com.rivada.events.service.CoreEventListener;
import com.rivada.events.service.model.ChainEvent;
import com.rivada.events.service.model.sqs.SqsMessageRequest;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class SqsDataMapper {
    public SqsMessageRequest<ChainEvent<?>> toSqsMessage(ChainEvent<?> chainEvent) {
        return SqsMessageRequest.<ChainEvent<?>>builder()
                .id(this.composeSqsId(chainEvent))
                .group(CoreEventListener.CHAIN_EVENTS_TOPIC)
                .message(chainEvent)
                .build();
    }

    private String composeSqsId(ChainEvent<?> chainEvent) {
        return chainEvent.getTxId() + "_" + chainEvent.getType();
    }
}
