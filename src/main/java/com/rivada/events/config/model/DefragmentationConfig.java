package com.rivada.events.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class DefragmentationConfig {

    private BigInteger blocksCountPerSecondEstimate;
    @JsonProperty("first")
    private DefragmentationTaskConfig firsTask;
    @JsonProperty("second")
    private DefragmentationTaskConfig secondTask;

    @Getter
    @Setter
    @Builder
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefragmentationTaskConfig {
        @JsonProperty("delay_min")
        private Long delayMin;
        @JsonProperty("timeout_min")
        private Long timeoutMin;
        @JsonProperty("events_age_scope_hours")
        private Integer eventsAgeScopeHours;
    }
}

