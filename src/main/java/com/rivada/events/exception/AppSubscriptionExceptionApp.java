package com.rivada.events.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class AppSubscriptionExceptionApp extends AppBaseDetailedException {
    public AppSubscriptionExceptionApp(String message, String detailedMessage) {
        super(message, detailedMessage);
    }

    public AppSubscriptionExceptionApp(Throwable throwable, Class classExc) {
        super(throwable.getMessage(), throwable.getLocalizedMessage());
    }
}
