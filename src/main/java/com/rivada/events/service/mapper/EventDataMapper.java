package com.rivada.events.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rivada.events.config.model.ContractsConfig;
import com.rivada.events.contract.RivadaCoreContract;
import com.rivada.events.db.entity.ChainEventEntity;
import com.rivada.events.service.enums.ChainEventType;
import com.rivada.events.service.model.*;
import com.rivada.events.service.util.NumericUtil;
import io.r2dbc.postgresql.codec.Json;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class EventDataMapper {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContractsConfig contractsConfig;

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "eventData", source = "model")
    public abstract ChainEventEntity toEntity(ChainEvent<?> model);

    protected Json convertEventDataToJson(@NotNull ChainEvent<?> source) {
        try {
            return Json.of(objectMapper.writeValueAsString(source));
        } catch (JsonProcessingException e) {
            log.error("Error occurred while serializing map to JSON: {}", source, e);
        }
        return Json.of("");
    }

    public ChainEvent<ChainWalletEvent> composeChainWalletLinkEvent(RivadaCoreContract.LinkWalletEventResponse event,
                                                                    LocalDateTime dateTimeTxn) {
        if (isBlank(event.wallet) || isBlank(event.userID)) {
            return null;
        }
        var type = ChainEventType.WALLET_LINK;
        var eventLog = event.log;
        var walletEvent = ChainWalletEvent.builder()
                .userId(event.userID)
                .userWalletAddress(event.wallet)
                .build();
        return this.composeChainEvent(eventLog.getTransactionHash(), eventLog.getBlockNumber().longValue(),
                walletEvent, dateTimeTxn, type);
    }

    public ChainEvent<ChainWalletEvent> composeChainWalletUnlinkEvent(RivadaCoreContract.UnlinkWalletEventResponse event,
                                                                      LocalDateTime dateTimeTxn) {
        if (isBlank(event.wallet) || isBlank(event.userID)) {
            return null;
        }
        var type = ChainEventType.WALLET_UNLINK;
        var eventLog = event.log;
        var walletEvent = ChainWalletEvent.builder()
                .userId(event.userID)
                .userWalletAddress(event.wallet)
                .build();
        return this.composeChainEvent(eventLog.getTransactionHash(), eventLog.getBlockNumber().longValue(),
                walletEvent, dateTimeTxn, type);
    }

    public ChainEvent<ChainLiquidityEvent> composeCapDepositEvent(RivadaCoreContract.DepositEventResponse event,
                                                                  LocalDateTime dateTimeTxn) {
        var type = ChainEventType.CAP_DEPOSIT;
        var eventLog = event.log;
        var liquidityData = ChainLiquidityEvent.builder()
                .userId(event.userID)
                .userWalletAddress(event.wallet)
                .tokenAddress(contractsConfig.getCapTokenAddress())
                .tokenSymbol(contractsConfig.getCapTokenSymbol())
                .amount(NumericUtil.getAmountFromDecimalAmount(event.amount, contractsConfig.getCapTokenDecimals()))
                .balanceAmount(NumericUtil.getAmountFromDecimalAmount(event.balanceAfter, contractsConfig.getCapTokenDecimals()))
                .discountAmount(NumericUtil.getAmountFromDecimalAmount(event.discountAfter, contractsConfig.getDiscountDecimals()))
                .build();
        return this.composeChainEvent(eventLog.getTransactionHash(), eventLog.getBlockNumber().longValue(),
                liquidityData, dateTimeTxn, type);
    }

    public ChainEvent<ChainLiquidityEvent> composeCapWithdrawEvent(RivadaCoreContract.WithdrawEventResponse event,
                                                                   LocalDateTime dateTimeTxn) {
        var type = ChainEventType.CAP_WITHDRAW;
        var eventLog = event.log;
        var liquidityData = ChainLiquidityEvent.builder()
                .userId(event.userID)
                .userWalletAddress(event.wallet)
                .tokenAddress(contractsConfig.getCapTokenAddress())
                .tokenSymbol(contractsConfig.getCapTokenSymbol())
                .amount(NumericUtil.getAmountFromDecimalAmount(event.amount, contractsConfig.getCapTokenDecimals()))
                .balanceAmount(NumericUtil.getAmountFromDecimalAmount(event.balanceAfter, contractsConfig.getCapTokenDecimals()))
                .discountAmount(NumericUtil.getAmountFromDecimalAmount(event.discountAfter, contractsConfig.getDiscountDecimals()))
                .build();
        return this.composeChainEvent(eventLog.getTransactionHash(), eventLog.getBlockNumber().longValue(),
                liquidityData, dateTimeTxn, type);
    }

    public ChainEvent<ChainBurnEvent> composeChainBurnEvent(RivadaCoreContract.BurnEventResponse event,
                                                            LocalDateTime dateTimeTxn) {
        var eventLog = event.log;
        var burnDataList = new ArrayList<ChainBurnData>();
        for (int i = 0; i < event.batchIDs.size(); i++) {
            var burnData = this.composeChainBurnEventData(event.seriesID, event.batchIDs.get(i), event.userIDs.get(i),
                    event.providerIDs.get(i), event.burnAmounts.get(i), event.balancesAfter.get(i), event.discounts.get(i),
                    event.rewardAmounts.get(i), event.feeCompensationAmounts.get(i));
            burnDataList.add(burnData);
        }
        var burnDataEvent = ChainBurnEvent.builder()
                .serialId(event.seriesID)
                .burnDataList(burnDataList)
                .build();
        return this.composeChainEvent(eventLog.getTransactionHash(), eventLog.getBlockNumber().longValue(),
                burnDataEvent, dateTimeTxn, ChainEventType.CAP_BURN);
    }

    private ChainBurnData composeChainBurnEventData(String serialId, BigInteger batchId, String userId, String providerId,
                                                    BigInteger amountBurned, BigInteger balanceAmount, BigInteger discountAmount,
                                                    BigInteger rewardAmount, BigInteger feeAmount) {
        return ChainBurnData.builder()
                .serialId(serialId)
                .batchId(batchId.longValue())
                .userId(userId)
                .providerId(providerId)
                .tokenAddress(contractsConfig.getCapTokenAddress())
                .tokenSymbol(contractsConfig.getCapTokenSymbol())
                .amount(NumericUtil.getAmountFromDecimalAmount(amountBurned, contractsConfig.getCapTokenDecimals()))
                .balanceAmount(NumericUtil.getAmountFromDecimalAmount(balanceAmount, contractsConfig.getCapTokenDecimals()))
                .discountAmount(NumericUtil.getAmountFromDecimalAmount(discountAmount, contractsConfig.getDiscountDecimals()))
                .feeAmount(NumericUtil.getAmountFromDecimalAmount(feeAmount, contractsConfig.getCapTokenDecimals()))
                .rewardAmount(NumericUtil.getAmountFromDecimalAmount(rewardAmount, contractsConfig.getCapTokenDecimals()))
                .build();
    }

    private <T> ChainEvent<T> composeChainEvent(String txId, Long blockNumber, T evenData, LocalDateTime dateTimeTxnTxn, ChainEventType type) {
        return ChainEvent.<T>builder()
                .txId(txId)
                .type(type)
                .eventData(evenData)
                .blockNumber(blockNumber)
                .dateTimeTxn(dateTimeTxnTxn)
                .importedAt(LocalDateTime.now())
                .build();
    }
}
