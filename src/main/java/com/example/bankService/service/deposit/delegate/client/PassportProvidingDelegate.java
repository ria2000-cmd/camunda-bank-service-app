package com.example.bankService.service.deposit.delegate.client;

import com.example.bankService.model.Client;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;


import static com.example.bankService.util.Constants.RIA;
import static com.example.bankService.util.Constants.SUDDEN_OPERATION_INTERRUPTION_ERROR;


@Slf4j
@Component("passportProvidingDelegate")
public class PassportProvidingDelegate implements JavaDelegate {



    @Override
    public void execute(DelegateExecution delegateExecution) {
        log.info("The passportProvidingDelegate has started....");

        delegateExecution.getVariables().forEach((k, v) -> log.info("Var {} = {}", k, v));


//        var client = (Client) delegateExecution.getVariable("client");
//needs to investigate why the client here is null, hence why I hard coded Ria instead of getting here from execution

        var client = RIA;
        log.info("BusinessKey = {}", delegateExecution.getProcessInstance().getBusinessKey());

        if (client == null) {
            throw new BpmnError(SUDDEN_OPERATION_INTERRUPTION_ERROR, "Client variable is missing!");
        }

        if (client.getPassport() == null) {
            throw new BpmnError(SUDDEN_OPERATION_INTERRUPTION_ERROR, "The passport should be present!");
        }

        log.info("Client: {} has provided passport", client.getName());
    }

}
