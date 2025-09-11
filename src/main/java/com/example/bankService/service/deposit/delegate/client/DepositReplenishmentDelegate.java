package com.example.bankService.service.deposit.delegate.client;

import com.example.bankService.model.Client;
import com.example.bankService.model.DepositContract;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.bankService.util.Constants.NOT_ENOUGH_MONEY;
import static com.example.bankService.util.Constants.RIA;

@Slf4j
@Component("depositReplenishmentDelegate")
public class DepositReplenishmentDelegate implements JavaDelegate {


    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        log.info("The depositReplenishmentDelegate has started....");

        // var client = (Client) delegateExecution.getVariable("client");
         //needs to investigate why the client here is null, hence why I hard coded Ria instead of getting here from execution

        var client = RIA;
        var preparedDepositContract  = (DepositContract) delegateExecution.getVariable("preparedDepositContract");

         if(!isClientHasEnoughMoney(client,preparedDepositContract)) {
             throw new BpmnError(NOT_ENOUGH_MONEY,
                     "Client does not have enough money to open a deposit ");
        }
    }


    private boolean isClientHasEnoughMoney(Client client, DepositContract preparedContract){

        var moneyOnWallet = client.getWallet().getMoneyCount();
        var depositMinimalSum = preparedContract.getMinimalSum();

        return moneyOnWallet.compareTo(depositMinimalSum) >= 0;
    }
}
