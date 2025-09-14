package com.example.bankService.util;

import com.example.bankService.model.Client;
import com.example.bankService.model.Wallet;

import java.math.BigDecimal;
public abstract class AbstractTestBase {

    protected static final String TEST_BUSINESS_KEY = "testBusinessKey";
    private static final Wallet WALLET_0 = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(0))
            .build();

    private static final Wallet WALLET_9 = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(9.99))
            .build();

    private static final Wallet WALLET_10 = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(10.00))
            .build();

    private static final Wallet WALLET_10_1 = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(10.01))
            .build();

    private static final Wallet WALLET_20 = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(20.00))
            .build();

    private static final Wallet WALLET_30 = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(30.00))
            .build();

    private static final Wallet WALLET_40 = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(40.00))
            .build();

    private static final Wallet WALLET_41 = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(41.00))
            .build();


    public static final Client CLIENT_MONEY_ZERO = Client.builder()
            .id("1")
            .wallet(WALLET_0)
            .build();

    public static final Client CLIENT_MONEY_9 = Client.builder()
            .id("1")
            .wallet(WALLET_9)
            .build();
    public static final Client CLIENT_MONEY_10= Client.builder()
            .id("1")
            .wallet(WALLET_10)
            .build();
    public static final Client CLIENT_MONEY__10_1 = Client.builder()
            .id("1")
            .wallet(WALLET_10_1)
            .build();
    public static final Client CLIENT_MONEY_20 = Client.builder()
            .id("1")
            .wallet(WALLET_20)
            .build();
    public static final Client CLIENT_MONEY_30 = Client.builder()
            .id("1")
            .wallet(WALLET_30)
            .build();
    public static final Client CLIENT_MONEY_40 = Client.builder()
            .id("1")
            .wallet(WALLET_40)
            .build();
    public static final Client CLIENT_MONEY_41 = Client.builder()
            .id("1")
            .wallet(WALLET_41)
            .build();

}
