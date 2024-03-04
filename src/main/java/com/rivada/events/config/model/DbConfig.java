package com.rivada.events.config.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class DbConfig {
    private String username;
    private String password;
    private String engine;
    private String database;
    private String host;
    private Integer port;
    private String dbClusterIdentifier;
    private String driver;
}
