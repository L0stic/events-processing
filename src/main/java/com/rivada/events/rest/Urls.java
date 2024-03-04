package com.rivada.events.rest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Urls {

    public static final String REST_VER = "/api";
    public static final String HEALTH_PATH = REST_VER + "/health";
    public static final String VERSION = REST_VER + "/version";
}
