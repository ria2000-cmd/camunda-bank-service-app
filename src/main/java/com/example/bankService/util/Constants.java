package com.example.bankService.util;

import com.example.bankService.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public class Constants {

    public static final String MAIN_DEPOSIT_CREDIT_PROCESS = "MainDepositCreditProcess";

    private static final Wallet RIA_WALLET = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(100.20))
            .build();


    private static final Wallet DAKIE_WALLET = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(300.20))
            .build();

    private static final Wallet ROOZEY_WALLET = Wallet.builder()
            .moneyCount(BigDecimal.valueOf(500.22))
            .build();



    private static final Passport RIA_PASSPORT = Passport.builder()
            .identicalNumber("KH123H123")
            .name("Ria")
            .surname("Maluta")
            .address("Lapwing")
            .birthDate(LocalDate.parse("2000-03-20"))
            .validFrom(LocalDate.parse("2021-11-11"))
            .validTo(LocalDate.parse("2031-11-11"))
            .build();

    private static final Passport ROOZEY_PASSPORT = Passport.builder()
            .identicalNumber("K123H246")
            .name("Roozey")
            .surname("mudau")
            .address("hops")
            .birthDate(LocalDate.parse("1996-09-17"))
            .validFrom(LocalDate.parse("2021-11-11"))
            .validTo(LocalDate.parse("2031-11-11"))
            .build();

    private static final Passport DAKIE_PASSPORT = Passport.builder()
            .identicalNumber("K1232565")
            .name("Daki")
            .surname("Maluta")
            .address("Square")
            .birthDate(LocalDate.parse("2007-02-15"))
            .validFrom(LocalDate.parse("2021-11-11"))
            .validTo(LocalDate.parse("2031-11-11"))
            .build();

    private static final Passport RENDY_PASSPORT = Passport.builder()
            .identicalNumber("K128965")
            .name("Rendy")
            .surname("Malta")
            .address("Riverside")
            .birthDate(LocalDate.parse("1996-10-18"))
            .validFrom(LocalDate.parse("2021-11-11"))
            .validTo(LocalDate.parse("2031-11-11"))
            .build();


    public static final Client RIA = Client.builder()
            .id("1")
            .name("Ria")
            .surname("Maluta")
            .address("Lapwing")
            .phoneNumber("+27632873563")
            .birthDate(LocalDate.parse("2000-03-20"))
            .wallet(RIA_WALLET)
            .passport(RIA_PASSPORT)
            .build();

    public static final Client ROOZEY = Client.builder()
            .id("2")
            .name("Roozey")
            .surname("Mudau")
            .address("hops")
            .phoneNumber("+2763756656563")
            .birthDate(LocalDate.parse("1996-09-17"))
            .wallet(ROOZEY_WALLET)
            .passport(ROOZEY_PASSPORT)
            .build();

    public static final Client DAKIE = Client.builder()
            .id("3")
            .name("Dakie")
            .surname("Maluts")
            .address("Square")
            .phoneNumber("+2763297896887")
            .birthDate(LocalDate.parse("2007-02-15"))
            .wallet(DAKIE_WALLET)
            .passport(DAKIE_PASSPORT)
            .build();

    public static final Client RENDY = Client.builder()
            .id("4")
            .name("Rendy")
            .surname("Malts")
            .address("riverside")
            .phoneNumber("+276326887")
            .birthDate(LocalDate.parse("1996-10-18"))
            .wallet(null)
            .passport(RENDY_PASSPORT)
            .build();


    public static final String  SUDDEN_OPERATION_INTERRUPTION_ERROR = "SUDDEN_OPERATION_INTERRUPTION_ERROR";
    public static final String VERIFICATION_SMS_NOT_OBTAINED = "VERIFICATION_SMS_NOT_OBTAINED";
    public static final String LIMIT_OF_VERIFICATION_SMS_ATTEMPTS_EXCEEDED = "LIMIT_OF_VERIFICATION_SMS_ATTEMPTS_EXCEEDED";
    public static final String NO_MORE_DEPOSITS_TO_OPEN = "NO_MORE_DEPOSITS_TO_OPEN";
    public static final String NOT_ENOUGH_MONEY = "NOT_ENOUGH_MONEY";

    private static final Deposit EARLY_SPRING = Deposit.builder()
            .name("Early-Spring")
            .currency("ZAR")
            .isCapitalized(true)
            .minimalSum(new BigDecimal("50.00"))
            .percentage(10.00)
            .currentSum(new BigDecimal("10500.00"))
            .termInMonth(3)
            .build();


    private static final Deposit HOT_SUMMER = Deposit.builder()
            .name("Hot-Summer")
            .currency("ZAR")
            .isCapitalized(true)
            .minimalSum(new BigDecimal("100.00"))
            .percentage(15.00)
            .currentSum(BigDecimal.ZERO)
            .termInMonth(6)
            .build();


    private static final Deposit HELLO_WINTER = Deposit.builder()
            .name("Hello-Winter")
            .currency("ZAR")
            .isCapitalized(false)
            .minimalSum(new BigDecimal("50.00"))
            .percentage(12.00)
            .currentSum(BigDecimal.ZERO)
            .termInMonth(9)
            .build();

    public static final List<Deposit>BANK_DEPOSITS = List.of(EARLY_SPRING, HOT_SUMMER, HELLO_WINTER);
    public static final List<Passport> BANK_ALREADY_CLIENTS_INFO = List.of(RIA_PASSPORT,DAKIE_PASSPORT);
    public static final List<Client> POLICE_WANTED_LIST = List.of(RENDY);
    public static final List<Client> BANK_BLACK_LIST = List.of(RENDY);

    public static final DepositContract blankDepositBLANK_DEPOSIT_CONTRACT = new DepositContract();



}
