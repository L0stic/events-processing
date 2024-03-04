package com.rivada.events.config;

import com.rivada.events.config.model.BlockchainConfig;
import com.rivada.events.config.model.BlockchainWalletConfig;
import com.rivada.events.config.model.ContractsConfig;
import com.rivada.events.service.AwsSecretsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@RequiredArgsConstructor
public class BlockchainConfiguration {

    @Value("${config.secrets.chain-data}")
    private String blockchainSecret;
    @Value("${config.secrets.chain-wallet}")
    private String blockchainWalletSecret;
    @Value("${config.secrets.contracts}")
    private String contractsSecret;
    final AwsSecretsService awsSecretsService;
    final Environment env;

    @Bean
    public BlockchainConfig blockchainConfig() {
        if(env.matchesProfiles("dev", "local")) {
            return BlockchainConfig.builder()
                    .networkEnvironmentType(env.getProperty("config.chain.networkEnvironmentType"))
                    .chainId(env.getProperty("config.chain.chainId"))
                    .rpcUrl(env.getProperty("config.chain.rpcUrl"))
                    .wssUri(env.getProperty("config.chain.wssUri"))
                    .gasTokenSymbol(env.getProperty("config.chain.gasTokenSymbol"))
                    .gasTokenDecimals(Integer.valueOf(env.getProperty("config.chain.gasTokenDecimals")))
                    .build();
        } else {
            return awsSecretsService.getSecret(blockchainSecret, BlockchainConfig.class);
        }
    }

    @Bean
    public BlockchainWalletConfig blockchainWalletConfig() {
        if(env.matchesProfiles("dev", "local")) {
            return BlockchainWalletConfig.builder()
                    .mainWalletPrivateKey(env.getProperty("config.chain-wallet.mainWalletPrivateKey"))
                    .build();
        } else {
            return awsSecretsService.getSecret(blockchainWalletSecret, BlockchainWalletConfig.class);
        }
    }

    @Bean
    public ContractsConfig contractsConfig() {
        if(env.matchesProfiles("dev", "local")) {
            return ContractsConfig.builder()
                    .coreContractAddress(env.getProperty("config.contracts.coreContractAddress"))
                    .governanceContractAddress(env.getProperty("config.contracts.governanceContractAddress"))
                    .capTokenAddress(env.getProperty("config.contracts.capTokenAddress"))
                    .oamTokenAddress(env.getProperty("config.contracts.oamTokenAddress"))
                    .capTokenSymbol(env.getProperty("config.contracts.capTokenSymbol"))
                    .build();
        } else {
            return awsSecretsService.getSecret(contractsSecret, ContractsConfig.class);
        }
    }
}