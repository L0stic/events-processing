package com.rivada.events.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class AppCriticalExceptionApp extends AppBaseDetailedException {
    public AppCriticalExceptionApp(String message, String detailedMessage) {
        super(message, detailedMessage);
    }

    public AppCriticalExceptionApp(Throwable throwable, Class classExc) {
        super(throwable.getMessage(), throwable.getLocalizedMessage());
    }
}
