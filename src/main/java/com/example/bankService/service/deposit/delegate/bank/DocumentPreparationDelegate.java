package com.example.bankService.service.deposit.delegate.bank;

import com.example.bankService.model.Client;
import com.example.bankService.model.Deposit;
import com.example.bankService.model.DepositContract;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.example.bankService.util.Constants.*;

@Slf4j
@Component("documentPreparationDelegate")
public class DocumentPreparationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        log.info("the DocumentPreparationDelegate has started.....");

        //needs to investigate why the client here is null, hence why I hard coded Ria instead of getting here from execution
//        var client = (Client) delegateExecution.getVariable("client");
          var client = RIA;

        var depositName = (String) delegateExecution.getVariable("depositName");
        var deposits = (List<Deposit>) delegateExecution.getVariable("bankDeposits");

        var choosenDeposit = deposits.stream()
                .filter(deposit -> deposit.getName().equals(depositName))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Deposit with name " + depositName + "is not exists"));

        //todo: call to DB for deposit contract obtaining
       var blankDepositContract = blankDepositBLANK_DEPOSIT_CONTRACT;

       var depositContract = fillDeposit(blankDepositContract, choosenDeposit, client);
        delegateExecution.setVariable("preparedDepositContract", depositContract);

    }

    private DepositContract fillDeposit(DepositContract blankDepositContract, Deposit choosenDeposit, Client client){
           var passport = client.getPassport();

        return blankDepositContract.setId(UUID.randomUUID())
                .setName(choosenDeposit.getName())
                .setMinimalSum(choosenDeposit.getMinimalSum())
                .setOpenDate(OffsetDateTime.now())
                .setCloseDate(OffsetDateTime.now().plusMonths(choosenDeposit.getTermInMonth().longValue()))

                .setClientName(passport.getName())
                .setClientSurName(passport.getSurname())
                .setClientPhoneNumber(client.getPhoneNumber());

    }
}
