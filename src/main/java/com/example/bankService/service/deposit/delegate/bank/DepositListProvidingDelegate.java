package com.example.bankService.service.deposit.delegate.bank;


import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.bankService.util.Constants.BANK_DEPOSITS;

@Slf4j
@Component("depositListProvidingDelegate")
public class DepositListProvidingDelegate implements JavaDelegate {


    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        log.info("the depositListProvidingDelegate has started.....");

        //todo obtain the list of deposits from DB
        log.info(String.format("The list of deposits provided by bank : %s", BANK_DEPOSITS));

        delegateExecution.setVariable("bankDeposits", BANK_DEPOSITS);



    }
}
