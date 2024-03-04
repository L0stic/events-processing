package com.rivada.events.service;

import com.rivada.events.config.model.BlockchainConfig;
import com.rivada.events.config.model.BlockchainWalletConfig;
import com.rivada.events.exception.AppCriticalExceptionApp;
import com.rivada.events.exception.ExceptionMessages;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;

@Slf4j
@Component
public class ArbitrumConnector {

    @Getter
    private ContractGasProvider contractGasProvider;
    private WebSocketService webSocketService;
    private HttpService httpService;
    @Getter
    private Web3j web3Wss;
    @Getter
    private Web3j web3Rpc;

    @Getter
    private Credentials mainCredentials;

    private final String wssUri;
    private final String rpcUrl;

    private final static int ACCESSIBILITY_REQUEST_TIMEOUT_SEC = 5;

    public ArbitrumConnector(BlockchainConfig blockchainConfig, BlockchainWalletConfig blockchainWalletConfig) throws IOException {
        this.wssUri = blockchainConfig.getWssUri();
        log.info("Try to connect to Arbitrum wss server: {}", wssUri);
        this.webSocketService = new WebSocketService(wssUri, true);
        webSocketService.connect();
        this.web3Wss = Web3j.build(webSocketService);
        log.info("Connected to Arbitrum WSS client version: {}", this.web3Wss.web3ClientVersion().send().getWeb3ClientVersion());

        this.rpcUrl = blockchainConfig.getRpcUrl();
        log.info("Try to connect to Arbitrum rpc server: {}", this.rpcUrl);
        this.httpService = new HttpService(this.rpcUrl, true);
        this.web3Rpc = Web3j.build(this.httpService);
        log.info("Connected to Arbitrum RPC client version: {}", this.web3Rpc.web3ClientVersion().send().getWeb3ClientVersion());

        contractGasProvider = new DefaultGasProvider();
        mainCredentials = Credentials.create(blockchainWalletConfig.getMainWalletPrivateKey());
    }

    public void reconnect() {
        log.warn("Try to reconnect {} blockchain.", this.getClass().getName());
        try {
            if (nonNull(this.webSocketService)) {
                this.webSocketService = new WebSocketService(this.wssUri, true);
                webSocketService.connect();
                this.web3Wss = Web3j.build(webSocketService);
            }
            if (nonNull(this.httpService)) {
                this.httpService = new HttpService(this.rpcUrl, true);
                this.web3Rpc = Web3j.build(this.httpService);
            }
        } catch (IOException e) {
            log.error("Can't connected to {} blockchain with URI: {}. Native exception is: {}",  this.getClass().getName(), this.wssUri, e.getMessage());
            return;
        }
        log.warn("Reconnection to {} blockchain was successful.", this.getClass().getName());
    }

    public void disposeComponent() {
        log.warn("Dispose command: Try to dispose chain connections on {}", this.getClass().getName());
        this.web3Wss.shutdown();
        if (this.webSocketService != null) {
            this.webSocketService.close();
        }
        this.web3Rpc.shutdown();
        log.warn("Dispose command: Chain connections on {} disposed successfully", this.getClass().getName());
    }

    public void checkAccessibility() throws AppCriticalExceptionApp {
        try {
            web3Wss.ethBlockNumber().sendAsync().get(ACCESSIBILITY_REQUEST_TIMEOUT_SEC, TimeUnit.SECONDS).getBlockNumber();
        } catch (Exception e) {
            throw new AppCriticalExceptionApp(ExceptionMessages.CHAIN_CONNECTOR_MAINTENANCE_EXCEPTION, e.getMessage());
        }
    }
}
