package com.example.bankService.service.deposit.delegate.client;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.example.bankService.util.Constants.BANK_DEPOSITS;
import static com.example.bankService.util.Constants.NO_MORE_DEPOSITS_TO_OPEN;

@Slf4j
@Component("depositChoosingCountDelegate")
public class DepositChoosingCountDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("The depositChoosingCountDelegate has started....");

       var depositChoosingCount = Optional.ofNullable((Long) delegateExecution.getVariable("depositChoosingCount"))
               .orElse(1L);

       if(depositChoosingCount == BANK_DEPOSITS.size()){
           throw new BpmnError(NO_MORE_DEPOSITS_TO_OPEN,
                   "The count of chances to deposit choosing is greater than number of bank deposits");
       }

       depositChoosingCount++;
       delegateExecution.setVariable("depositChoosingCount", depositChoosingCount);

    }
}
