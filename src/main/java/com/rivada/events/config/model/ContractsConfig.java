package com.rivada.events.config.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class ContractsConfig {
    @JsonProperty("core_contract_address")
    private String coreContractAddress;
    @JsonProperty("governance_contract_address")
    private String governanceContractAddress;
    @JsonProperty("cap_token_address")
    private String capTokenAddress;
    @JsonProperty("oam_token_address")
    private String oamTokenAddress;
    @JsonProperty("cap_token_symbol")
    private String capTokenSymbol;
    @Builder.Default
    @JsonProperty("cap_token_decimals")
    private Integer capTokenDecimals = 18;
    @Builder.Default
    @JsonProperty("discount_decimals")
    private Integer discountDecimals = 4;
}
