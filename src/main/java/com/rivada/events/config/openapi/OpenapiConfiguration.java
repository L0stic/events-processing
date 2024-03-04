package com.rivada.events.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenapiConfiguration {

    private final BuildProperties buildProperties;

    @Bean
    OpenAPI openApi(@Value("${spring.application.name}") String appName) {
       return new OpenAPI()
               .info(
                   new Info().title(appName).version(this.buildProperties.getVersion())
               );
    }

}
