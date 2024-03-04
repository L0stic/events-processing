package com.rivada.events.rest.system;


import com.rivada.events.config.openapi.ApiTag;
import com.rivada.events.rest.Urls;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = ApiTag.SYSTEM)
public interface SystemController {

    @GetMapping(value = Urls.VERSION, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get application version", operationId = "getVersion")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Version info")
    })
    VersionResponse getVersion();

    @GetMapping(value = Urls.HEALTH_PATH, produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Get application health", operationId = "getHealth")
    @ResponseStatus(value = HttpStatus.OK)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Health info")
    })
    String getHealth();
}
