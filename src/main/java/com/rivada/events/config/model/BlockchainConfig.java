package com.rivada.events.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockchainConfig {
    @JsonProperty("network_environment_type")
    private String networkEnvironmentType;
    @JsonProperty("chain_id")
    private String chainId;
    @JsonProperty("rpc_url")
    private String rpcUrl;
    @JsonProperty("wss_uri")
    private String wssUri;
    @JsonProperty("gas_token_symbol")
    private String gasTokenSymbol;
    @JsonProperty("gas_token_decimals")
    private Integer gasTokenDecimals;
}
