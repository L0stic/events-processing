package com.rivada.events.db.repository;

import com.rivada.events.db.entity.ChainEventEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.util.List;

@Repository
public interface ChainEventRepository extends R2dbcRepository<ChainEventEntity, Long> {
    Mono<ChainEventEntity> findByTxId(String txId);

    @Query("select not exists (select id from chain_event where tx_id = :tx)")
    Mono<Boolean> notExistsByTxId(@Param("tx") String txId);

    @Query(value = "select min(max_block) as block from (select coalesce(B.max_block, 0) as max_block from (select :types AS type) A left join (SELECT max(block_number) as max_block, type FROM chain_event GROUP BY type) B on A.type = B.type) q")
    Mono<BigInteger> findLastProcessedBlockThroughAllEvents(@Param("types") List<String> types);
}
