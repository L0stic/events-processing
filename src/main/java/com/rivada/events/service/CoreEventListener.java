package com.rivada.events.service;


import com.rivada.events.config.model.ContractsConfig;
import com.rivada.events.config.model.DefragmentationConfig;
import com.rivada.events.contract.RivadaCoreContract;
import com.rivada.events.exception.AppCriticalExceptionApp;
import com.rivada.events.exception.AppSubscriptionExceptionApp;
import com.rivada.events.exception.ExceptionMessages;
import com.rivada.events.service.mapper.EventDataMapper;
import com.rivada.events.service.mapper.SqsDataMapper;
import com.rivada.events.service.model.ChainEvent;
import com.rivada.events.service.util.DateTimeUtil;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthBlock;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@Slf4j
@RequiredArgsConstructor
@Service
public class CoreEventListener {
    private final ArbitrumConnector arbitrumConnector;
    private final AwsSQSPublisherConnector sqsPublisherConnector;
    private final ChainEventService chainEventService;
    private final SqsDataMapper sqsDataMapper;
    private final EventDataMapper eventDataMapper;
    private final ContractsConfig contractsConfig;
    private final Validator validator;
    private final DefragmentationConfig defragmentationConfig;
    private final AtomicBoolean isAppStart = new AtomicBoolean(false);
    private final AtomicLong appOkTimestamp = new AtomicLong(0);
    private RivadaCoreContract rivadaCoreContract;
    private ScheduledExecutorService defragmentationExecutor;
    private ScheduledExecutorService accessibilityExecutor;
    private List<Disposable> subscriptions;
    private CopyOnWriteArrayList<Disposable> defragmentationSubscriptions;

    @Value("${config.accessibilityCheck.timeoutSec}")
    private Long accessibilityTimeoutSec;
    @Value("${config.accessibilityCheck.criticalTimeoutSec}")
    private Long criticalAccessibilityTimeoutSec;
    private final static Long EVENT_LISTENING_INITIAL_DELAY_SEC = 2L;
    private final AtomicLong defragmentationEventsTaskIndexer = new AtomicLong(0);
    public final static String CHAIN_EVENTS_TOPIC = "chainEvent";
    public final static String DEFRAGMENTATION_LOG_PREFIX_FORMAT = "task-%s. defragAgeHour-%s => ";
    public final static String INITIAL_LISTENER_DEFRAGMENTATION_LOG_PREFIX_FORMAT = "Initial";

    @PostConstruct
    private void initListener() {
        rivadaCoreContract = RivadaCoreContract.load(contractsConfig.getCoreContractAddress(), arbitrumConnector.getWeb3Wss(),
                arbitrumConnector.getMainCredentials(), arbitrumConnector.getContractGasProvider());
        Mono.delay(Duration.of(EVENT_LISTENING_INITIAL_DELAY_SEC, ChronoUnit.SECONDS))// little timeout to init DB connection
                .flatMap((s) -> chainEventService.findLastProcessedBlock())
                .map(lastKnownBlock -> {
                    log.info("Start ChainEventListener with LastKnownBlock: {}", lastKnownBlock);
                    var mainEthFilter = new EthFilter(DefaultBlockParameter.valueOf(lastKnownBlock.add(BigInteger.ONE)),
                            DefaultBlockParameterName.LATEST, contractsConfig.getCoreContractAddress());
                    this.subscriptions = new ArrayList<>(List.of(
                            createCapBurnSubscription(mainEthFilter, INITIAL_LISTENER_DEFRAGMENTATION_LOG_PREFIX_FORMAT)
                            , createCapDepositSubscription(mainEthFilter, INITIAL_LISTENER_DEFRAGMENTATION_LOG_PREFIX_FORMAT)
                            , createCapWithdrawSubscription(mainEthFilter, INITIAL_LISTENER_DEFRAGMENTATION_LOG_PREFIX_FORMAT)
                            , createWalletLinkSubscription(mainEthFilter, INITIAL_LISTENER_DEFRAGMENTATION_LOG_PREFIX_FORMAT)
                            , createWalletUnlinkSubscription(mainEthFilter, INITIAL_LISTENER_DEFRAGMENTATION_LOG_PREFIX_FORMAT)
                    ));
                    return "";
                })
                .subscribe((s) -> {
                            isAppStart.set(true);
                            log.info("We initiated ChainEventListener successfully");
                        }
                );
        this.accessibilityExecutor = Executors.newSingleThreadScheduledExecutor();
        accessibilityExecutor.scheduleAtFixedRate(new CheckAccessibilityTask(), accessibilityTimeoutSec, accessibilityTimeoutSec, TimeUnit.SECONDS);
        log.info("We initiated CheckAccessibilityTask successfully");

        //every 1 day we make defragmentation listening of chain events. Initial delay 1 hour
        this.defragmentationExecutor = Executors.newSingleThreadScheduledExecutor();
        this.defragmentationSubscriptions = new CopyOnWriteArrayList<>();
        var firstDefragTask = defragmentationConfig.getFirsTask();
        defragmentationExecutor.scheduleAtFixedRate(
                new EventDefragmentationTask(firstDefragTask.getEventsAgeScopeHours()),
                firstDefragTask.getDelayMin(), firstDefragTask.getTimeoutMin(), TimeUnit.MINUTES);
        defragmentationExecutor.scheduleAtFixedRate(
                new EventDefragmentationTask(firstDefragTask.getEventsAgeScopeHours()),
                firstDefragTask.getDelayMin(), firstDefragTask.getTimeoutMin(), TimeUnit.MINUTES);
        log.info("We initiated EventDefragmentationTask successfully");
    }

    protected class CheckAccessibilityTask implements Runnable {
        @Override
        public void run() {
            try {
                if (isAppStart.get()) {
                    try {
                        arbitrumConnector.checkAccessibility();
                        appOkTimestamp.set(System.currentTimeMillis());
                        log.info("Accessibility Check: Connector for chain '{}' is OK.", arbitrumConnector.getClass());
                    } catch (Exception e) {
                        log.error("Accessibility Check: Connector for chain '{}' is not accessible. Error for connection is: {}. Stack trace", arbitrumConnector.getClass(), e.getMessage(), e);
                        exceptionNotificationHandler(e);
                    }
                } else if (appOkTimestamp.get() > 0 && System.currentTimeMillis() - appOkTimestamp.get() > criticalAccessibilityTimeoutSec * 1000) {
                    log.error("\n!!!--!!!\nBlockchain listeners components status is not OK more than {} sec). Shutdown application.\n!!!--!!!", criticalAccessibilityTimeoutSec);
                    System.exit(1);
                }
            } catch (Exception e) {
                log.error("Unexpected error in running CheckAccessibilityTask error: {}", e.getMessage(), e);
            }
        }
    }

    private void exceptionNotificationHandler(Exception e) {
        if (nonNull(e) && e instanceof AppCriticalExceptionApp) {
            isAppStart.set(false);
            log.warn("\n!!!--!!!\nWe've caught condition that prevents further blockchain events listening. Try to reconnect\n!!!--!!!");
            disposeComponent();
            arbitrumConnector.reconnect();
            initListener();
        } else {
            log.warn("New unexpected error in listening connections: {}", e.getMessage());
        }
    }

    public void disposeComponent() {
        log.warn("Dispose command: Try to dispose Rivada Core '{}' subscriptions", contractsConfig.getCoreContractAddress());
        if (isNotEmpty(this.subscriptions)) {
            this.subscriptions.parallelStream().forEach(Disposable::dispose);
            this.subscriptions.clear();
        }
        if (isNotEmpty(this.defragmentationSubscriptions)) {
            this.defragmentationSubscriptions.parallelStream().forEach(Disposable::dispose);
            this.defragmentationSubscriptions.clear();
        }
        log.warn("Dispose command: Rivada Core '{}' subscription was successfully disposed", contractsConfig.getCoreContractAddress());
        arbitrumConnector.disposeComponent();
        defragmentationExecutor.shutdown();
        accessibilityExecutor.shutdown();
    }

    @AllArgsConstructor
    protected class EventDefragmentationTask implements Runnable {
        Integer eventsAgeInHours;

        @Override
        public void run() {
            var taskIndex = defragmentationEventsTaskIndexer.getAndIncrement();
            try {
                if (isNotEmpty(defragmentationSubscriptions)) {
                    defragmentationSubscriptions.parallelStream().forEach(Disposable::dispose);
                    defragmentationSubscriptions.clear();
                }
                var defragmentationFilter = composeEthFilterForDefragmentationTask(eventsAgeInHours);
                if (isNull(defragmentationFilter)) {
                    return;
                }
                var logPrefix = DEFRAGMENTATION_LOG_PREFIX_FORMAT.formatted(taskIndex, eventsAgeInHours);
                defragmentationSubscriptions.add(createCapDepositSubscription(defragmentationFilter, logPrefix));
                defragmentationSubscriptions.add(createCapWithdrawSubscription(defragmentationFilter, logPrefix));
                defragmentationSubscriptions.add(createCapBurnSubscription(defragmentationFilter, logPrefix));
                defragmentationSubscriptions.add(createWalletLinkSubscription(defragmentationFilter, logPrefix));
                defragmentationSubscriptions.add(createWalletUnlinkSubscription(defragmentationFilter, logPrefix));
            } catch (Exception e) {
                log.error("Unexpected error in running EventDefragmentationTask index:{}, error: {}", taskIndex, e.getMessage(), e);
            }
        }
    }

    private EthFilter composeEthFilterForDefragmentationTask(Integer eventsAgeInHours) {
        EthBlock.Block currentBlock;
        EthBlock.Block startBlock;
        try {
            // Get the current block number
            currentBlock = arbitrumConnector.getWeb3Rpc()
                    .ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                    .send()
                    .getBlock();

            // Calculate the block number from a week ago
            BigInteger startBlockNumberEstimate = currentBlock.getNumber()
                    .subtract(BigInteger.valueOf(eventsAgeInHours * 60 * 60).multiply(defragmentationConfig.getBlocksCountPerSecondEstimate()));

            // Get the block from a week ago
            startBlock = arbitrumConnector.getWeb3Rpc()
                    .ethGetBlockByNumber(DefaultBlockParameter.valueOf(startBlockNumberEstimate), true)
                    .send()
                    .getBlock();
        } catch (IOException e) {
            log.error("Can't load chain block details for eventDefragmentationTask. Original message: {}", e.getMessage(), e);
            return null;
        }

        return new EthFilter(DefaultBlockParameter.valueOf(startBlock.getNumber()),
                DefaultBlockParameter.valueOf(currentBlock.getNumber()), contractsConfig.getCoreContractAddress());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Disposable createWalletLinkSubscription(EthFilter filter, String logPrefix) {
        // subscribe to chain events
        return Flux.from(this.rivadaCoreContract.linkWalletEventFlowable(filter))
                .filter(Optional::isPresent)
                .filterWhen(event -> chainEventService.notExistsByTxId(event.get().log.getTransactionHash()))
                .flatMap(event -> Mono.zip(Mono.just(event.get()), this.getBlockDateTime(event.get().log.getBlockHash())))
                .flatMap(tuple -> {
                    var event = tuple.getT1();
                    var dateTimeTxn = tuple.getT2();
                    log.trace("{} - catch link event tx: {}. Start processing.", logPrefix, event.log.getTransactionHash());
                    var chainEvent = eventDataMapper.composeChainWalletLinkEvent(event, dateTimeTxn);
                    Set<ConstraintViolation<ChainEvent<?>>> violations = validator.validate(chainEvent);
                    if (isNull(chainEvent) || !violations.isEmpty()) {
                        return Mono.empty();
                    }
                    //send data to MessageBroker
                    return sqsPublisherConnector.sendMessage(sqsDataMapper.toSqsMessage(chainEvent))
                            //save data to DB
                            .flatMap(response -> chainEventService.saveEvent(chainEvent));
                })
                .subscribe(tx -> {
                            log.trace("{} - Message for new link wallet event with txId: {} was created successfully.", logPrefix, tx);
                        },
                        throwable -> {
                            var message = "Got error while filter linkWalletEventFlowable for core contract: %s"
                                    .formatted(contractsConfig.getCoreContractAddress());
                            log.error("{}.\nNative exception: {}", message, throwable.getMessage(), throwable);
                            throw new AppSubscriptionExceptionApp(message, throwable.getMessage());
                        });
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Disposable createWalletUnlinkSubscription(EthFilter filter, String logPrefix) {
        // subscribe to chain events
        return Flux.from(this.rivadaCoreContract.unlinkWalletEventFlowable(filter))
                .filter(Optional::isPresent)
                .filterWhen(event -> chainEventService.notExistsByTxId(event.get().log.getTransactionHash()))
                .flatMap(event -> Mono.zip(Mono.just(event.get()), this.getBlockDateTime(event.get().log.getBlockHash())))
                .flatMap(tuple -> {
                    var event = tuple.getT1();
                    var dateTimeTxn = tuple.getT2();
                    log.trace("{} - catch unlink event tx: {}. Start processing.", logPrefix, event.log.getTransactionHash());
                    var chainEvent = eventDataMapper.composeChainWalletUnlinkEvent(event, dateTimeTxn);
                    Set<ConstraintViolation<ChainEvent<?>>> violations = validator.validate(chainEvent);
                    if (isNull(chainEvent) || !violations.isEmpty()) {
                        return Mono.empty();
                    }
                    //send data to MessageBroker
                    return sqsPublisherConnector.sendMessage(sqsDataMapper.toSqsMessage(chainEvent))
                            //save data to DB
                            .flatMap(response -> chainEventService.saveEvent(chainEvent));
                })
                .subscribe(tx -> {
                            log.trace("{} - Message for new unlink wallet event with txId: {} was created successfully.", logPrefix, tx);
                        },
                        throwable -> {
                            var message = "Got error while filter unlinkWalletEventFlowable for core contract: %s"
                                    .formatted(contractsConfig.getCoreContractAddress());
                            log.error("{}.\nNative exception: {}", message, throwable.getMessage(), throwable);
                            throw new AppSubscriptionExceptionApp(message, throwable.getMessage());
                        });
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Disposable createCapDepositSubscription(EthFilter filter, String logPrefix) {
        // subscribe to chain events
        return Flux.from(this.rivadaCoreContract.depositEventFlowable(filter))
                .filter(Optional::isPresent)
                .filterWhen(event -> chainEventService.notExistsByTxId(event.get().log.getTransactionHash()))
                .flatMap(event -> Mono.zip(Mono.just(event.get()), this.getBlockDateTime(event.get().log.getBlockHash())))
                .flatMap(tuple -> {
                    var event = tuple.getT1();
                    var dateTimeTxn = tuple.getT2();
                    log.trace("{} - catch deposit event tx: {}. Start processing.", logPrefix, event.log.getTransactionHash());
                    var chainEvent = eventDataMapper.composeCapDepositEvent(event, dateTimeTxn);
                    Set<ConstraintViolation<ChainEvent<?>>> violations = validator.validate(chainEvent);
                    if (isNull(chainEvent) || !violations.isEmpty()) {
                        return Mono.empty();
                    }
                    //send data to MessageBroker
                    return sqsPublisherConnector.sendMessage(sqsDataMapper.toSqsMessage(chainEvent))
                            //save data to DB
                            .flatMap(response -> chainEventService.saveEvent(chainEvent));
                })
                .subscribe(tx -> {
                            log.trace("{} - Message for new deposit event with txId: {} was created successfully.", logPrefix, tx);
                        },
                        throwable -> {
                            var message = "Got error while filter depositEventFlowable for core contract: %s"
                                    .formatted(contractsConfig.getCoreContractAddress());
                            log.error("{}.\nNative exception: {}", message, throwable.getMessage(), throwable);
                            throw new AppSubscriptionExceptionApp(message, throwable.getMessage());
                        });
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private Disposable createCapWithdrawSubscription(EthFilter ethFilter, String logPrefix) {
        // subscribe to chain events
        return Flux.from(this.rivadaCoreContract.withdrawEventFlowable(ethFilter))
                .filter(Optional::isPresent)
                .filterWhen(event -> chainEventService.notExistsByTxId(event.get().log.getTransactionHash()))
                .flatMap(event -> Mono.zip(Mono.just(event.get()), this.getBlockDateTime(event.get().log.getBlockHash())))
                .flatMap(tuple -> {
                    var event = tuple.getT1();
                    var dateTimeTxn = tuple.getT2();
                    log.trace("{} - catch withdraw event tx: {}. Start processing.", logPrefix, event.log.getTransactionHash());
                    var chainEvent = eventDataMapper.composeCapWithdrawEvent(event, dateTimeTxn);
                    Set<ConstraintViolation<ChainEvent<?>>> violations = validator.validate(chainEvent);
                    if (isNull(chainEvent) || !violations.isEmpty()) {
                        return Mono.empty();
                    }
                    //send data to MessageBroker
                    return sqsPublisherConnector.sendMessage(sqsDataMapper.toSqsMessage(chainEvent))
                            //save data to DB
                            .flatMap(response -> chainEventService.saveEvent(chainEvent));
                })
                .subscribe(tx -> {
                            log.trace("{} - Message for new withdraw event with txId: {} was created successfully.", logPrefix, tx);
                        },
                        throwable -> {
                            var message = "Got error while filter withdrawEventFlowable for core contract: %s"
                                    .formatted(contractsConfig.getCoreContractAddress());
                            log.error("{}.\nNative exception: {}", message, throwable.getMessage(), throwable);
                            throw new AppSubscriptionExceptionApp(message, throwable.getMessage());
                        });
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private reactor.core.Disposable createCapBurnSubscription(EthFilter ethFilter, String logPrefix) {
        // subscribe to chain events
        return Flux.from(this.rivadaCoreContract.burnEventFlowable(ethFilter))
                .filter(Optional::isPresent)
                .filterWhen(event -> chainEventService.notExistsByTxId(event.get().log.getTransactionHash()))
                .flatMap(event -> Mono.zip(Mono.just(event.get()), this.getBlockDateTime(event.get().log.getBlockHash())))
                .flatMap(tuple -> {
                    var event = tuple.getT1();
                    var dateTimeTxn = tuple.getT2();
                    log.trace("{} - catch burn event tx: {}. Start processing.", logPrefix, event.log.getTransactionHash());
                    var chainEvent = eventDataMapper.composeChainBurnEvent(event, dateTimeTxn);
                    Set<ConstraintViolation<ChainEvent<?>>> violations = validator.validate(chainEvent);
                    if (isNull(chainEvent) || !violations.isEmpty()) {
                        return Mono.empty();
                    }
                    //send data to MessageBroker
                    return sqsPublisherConnector.sendMessage(sqsDataMapper.toSqsMessage(chainEvent))
                            //save data to DB
                            .flatMap(response -> chainEventService.saveEvent(chainEvent));
                })
                .subscribe(tx -> {
                            log.trace("{} - Message for new burn event with txId: {} was created successfully.", logPrefix, tx);
                        },
                        throwable -> {
                            var message = "Got error while filter burnEventFlowable for core contract: %s"
                                    .formatted(contractsConfig.getCoreContractAddress());
                            log.error("{}.\nNative exception: {}", message, throwable.getMessage(), throwable);
                            throw new AppSubscriptionExceptionApp(message, throwable.getMessage());
                        });
    }

    private Mono<LocalDateTime> getBlockDateTime(String blockHash) {
        return Mono.just(getBlockDateTimeRaw(blockHash, false));
    }

    @SneakyThrows
    private LocalDateTime getBlockDateTimeRaw(String blockHash, boolean retryAttempt) {
        EthBlock.Block block;
        try {
            block = arbitrumConnector.getWeb3Rpc()
                    .ethGetBlockByHash(blockHash, false)
                    .send().getBlock();
        } catch (IOException e) {
            if (!retryAttempt) {
                log.debug("Retry to get block dateTime for blockHash {}. Native exception was: \n {}", blockHash, e.getMessage());
                Thread.sleep(accessibilityTimeoutSec * 1000L);
                return this.getBlockDateTimeRaw(blockHash, true);
            }
            log.error("Can't load chain block details for blockHash: {}. Original message: {}", e.getMessage(), blockHash);
            throw new AppSubscriptionExceptionApp(ExceptionMessages.CHAIN_CONNECTOR_MAINTENANCE_EXCEPTION,
                    "Responded with null block for block hash: " + blockHash + " skip processing event");
        }
        return DateTimeUtil.getDateTime(block.getTimestamp().longValue());
    }

}
