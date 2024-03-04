package com.rivada.events.service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@ToString
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ChainLiquidityEvent extends BaseChainEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 2419929360981910077L;

    private String tokenAddress;

    private String tokenSymbol;

    private BigDecimal amount;

    private BigDecimal balanceAmount;

    private BigDecimal discountAmount;
}
