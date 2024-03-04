package com.rivada.events.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum ChainEventType {
    CAP_DEPOSIT("Stake CAP tokens to contract and buy Traffic"),
    CAP_WITHDRAW("Unstake CAP tokens from contract and close Traffic"),
    CAP_BURN("Burn CAP tokens according used Traffic"),

    WALLET_LINK("Link new wallet to user account"),
    WALLET_UNLINK("Unlink current wallet from user account")
    ;

    private final String description;

    public static List<String> getAllValueNames(){
        return Arrays.stream(ChainEventType.values()).parallel().map(ChainEventType::name).toList();
    }
}
