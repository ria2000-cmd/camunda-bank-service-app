package com.example.bankService.service.way.delegate;

import com.example.bankService.model.Client;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component("taxiPaymentDelegate")
public class TaxiPaymentDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("the taxiPaymentDelegate has started.....");

        var client = (Client) delegateExecution.getVariable("client");
        var taxiCost = (String) delegateExecution.getVariable("taxiCost");

        var moneyOnWallet = client.getWallet().getMoneyCount().subtract(new BigDecimal(taxiCost));
        log.info(String.format("Client just has paid on the taxi about %s", moneyOnWallet));

        client.getWallet().setMoneyCount(moneyOnWallet);

        delegateExecution.setVariable("client", client);

    }
}
