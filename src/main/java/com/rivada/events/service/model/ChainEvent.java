package com.rivada.events.service.model;

import com.rivada.events.service.enums.ChainEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@ToString
@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ChainEvent<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -1659087778464087867L;

    @NotNull
    private ChainEventType type;

    @NotBlank
    private String txId;

    @NotNull
    private T eventData;

    @NotNull
    private Long blockNumber;

    @NotNull
    private LocalDateTime dateTimeTxn;

    private LocalDateTime importedAt;
}
