package com.rivada.events.service.model;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@ToString
@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder
@NoArgsConstructor
public class ChainBurnEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = -4066788394950528375L;

    private String serialId;

    private List<ChainBurnData> burnDataList;
}