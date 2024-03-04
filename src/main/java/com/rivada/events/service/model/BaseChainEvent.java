package com.rivada.events.service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@ToString
@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BaseChainEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 6216669993856076185L;

    private String userId;
    private String userWalletAddress;
}
