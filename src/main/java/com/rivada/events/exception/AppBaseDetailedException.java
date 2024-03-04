package com.rivada.events.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.BAD_REQUEST)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class AppBaseDetailedException extends RuntimeException {

    private String detailedMessage;

    public AppBaseDetailedException(String message) {
        super(message);
    }

    public AppBaseDetailedException(String message, String detailedMessage) {
        super(message);
        this.detailedMessage = detailedMessage;
    }
}
