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
@NoArgsConstructor
public class ChainBurnData extends ChainLiquidityEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 4212844218008567576L;

    private String serialId;

    private Long batchId;

    private String providerId;

    private BigDecimal rewardAmount;

    private BigDecimal feeAmount;
}