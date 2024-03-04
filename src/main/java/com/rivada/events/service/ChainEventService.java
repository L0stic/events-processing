package com.rivada.events.service;

import com.rivada.events.db.entity.ChainEventEntity;
import com.rivada.events.db.repository.ChainEventRepository;
import com.rivada.events.service.enums.ChainEventType;
import com.rivada.events.service.mapper.EventDataMapper;
import com.rivada.events.service.model.ChainEvent;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigInteger;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChainEventService {
    private final ChainEventRepository chainEventRepository;
    private final EventDataMapper eventDataMapper;

    public Mono<BigInteger> findLastProcessedBlock() {
        return chainEventRepository.findLastProcessedBlockThroughAllEvents(ChainEventType.getAllValueNames());
    }

    /**
     * Save new chain event to DB
     * @return chain event Transaction ID
     */
    public Mono<String> saveEvent(@NotNull ChainEvent<?> chainEvent) {
        var chainEventEntity = eventDataMapper.toEntity(chainEvent);
        return chainEventRepository.save(chainEventEntity)
                .onErrorResume(e -> {
                            log.warn("new chain event type: {} wasn't saved and perhaps already existed for data: txId: '{}'. Original error '{}'",
                                    chainEvent.getType(), chainEvent.getTxId(), e.toString());
                            return this.findChainEventByTxHash(chainEvent.getTxId());
                        }
                )
                .map(ChainEventEntity::getTxId);
    }

    /**
     * Check if event already exists
     *
     * @return Mono(true) if event doesn't exists and we need to save, Mono(false) if already exists
     */
    public Mono<Boolean> notExistsByTxId(String txId){
        return chainEventRepository.notExistsByTxId(txId);
    }

    public Mono<ChainEventEntity> findChainEventByTxHash(String txId) {
        return chainEventRepository.findByTxId(txId);
    }
}
