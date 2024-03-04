package com.rivada.events.db.entity;

import com.rivada.events.service.enums.ChainEventType;
import io.r2dbc.postgresql.codec.Json;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(value = "chain_event")
public class ChainEventEntity extends BaseEntity {

    @Column(value = "tx_id")
    private String txId;

    @Column(value = "type")
    private ChainEventType type;

    @Column(value = "event_data")
    private Json eventData;

    @Column(value = "date_time_txn")
    private LocalDateTime dateTimeTxn;

    @Column(value = "block_number")
    private Long blockNumber;
}
