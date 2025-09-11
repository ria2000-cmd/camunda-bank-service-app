package com.example.bankService.service.deposit.delegate.bank;

import com.example.bankService.model.Client;
import com.example.bankService.model.DepositContract;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.example.bankService.util.Constants.NOT_ENOUGH_MONEY;

@Slf4j
@Component("moneyCountVerificationDelegate")
public class MoneyCountVerificationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("The MoneyCountVerificationDelegate has started....");

        var paidMoneyInt = (Integer) delegateExecution.getVariable("paidMoney");
        var paidMoney = BigDecimal.valueOf(paidMoneyInt);
        var preparedDepositContract  = (DepositContract) delegateExecution.getVariable("preparedDepositContract");
        var minimalSumToReplenish = preparedDepositContract.getMinimalSum();


        if(isClientPaidEnoughMoney(paidMoney,minimalSumToReplenish)) {
            putMoneyToSafe(paidMoney);

        }else {
            throw new BpmnError(NOT_ENOUGH_MONEY,
                    "Client does not have enough money to deposit replenish");
        }

    }

    private boolean isClientPaidEnoughMoney(BigDecimal transferredMoneySum, BigDecimal minimalSumToReplenish){
        log.info("The money paid by client is ....");
        return transferredMoneySum.compareTo(minimalSumToReplenish) >= 0;
    }

    private void putMoneyToSafe(BigDecimal paidMoney){
        log.info(String.format("Bank worker has put the obtained money %s from the client to a safe ", paidMoney));
    }
}
