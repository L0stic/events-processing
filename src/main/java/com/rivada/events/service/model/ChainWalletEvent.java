package com.rivada.events.service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@ToString
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
public class ChainWalletEvent extends BaseChainEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 2419929360981910077L;
}
