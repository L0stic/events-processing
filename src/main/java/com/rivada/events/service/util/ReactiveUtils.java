package com.rivada.events.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.Optional;
import java.util.logging.Level;

@Slf4j
@Component
public class ReactiveUtils {

    @SuppressWarnings("rawtypes")
    public static <T> Mono<T> processWithLog(Class className, Mono<T> monoToLog) {
        return monoToLog
                .log(className.getName() + '.', Level.INFO, SignalType.ON_NEXT, SignalType.ON_COMPLETE);
    }

    @SuppressWarnings("rawtypes")
    public static <T> Flux<T> processWithLog(Class className, Flux<T> fluxToLog) {
        return fluxToLog
                .log(className.getName() + '.', Level.INFO, SignalType.ON_NEXT, SignalType.ON_COMPLETE);
    }

    public static <T> Mono<Optional<T>> optional(Mono<T> in) {
        return in.map(Optional::of).switchIfEmpty(Mono.just(Optional.empty()));
    }
}
