package com.rivada.events.rest.system;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SystemControllerImpl implements SystemController {

    private final BuildProperties buildProperties;

    @Override
    public VersionResponse getVersion() {
        return new VersionResponse(buildProperties.getVersion());
    }

    @Override
    public String getHealth() {
        return "OK";
    }

}
