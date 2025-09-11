package com.example.bankService.service.deposit.delegate.client;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.bankService.util.Constants.VERIFICATION_SMS_NOT_OBTAINED;

@Slf4j
@Component("smsObtainingByClientDelegate")
public class SmsObtainingByClientDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("The smsObtainingByClientDelegate has started....");

        var variables = delegateExecution.getVariables();

        if(!variables.containsKey("sendMobileCode")){
            throw new BpmnError(VERIFICATION_SMS_NOT_OBTAINED, "The verification sms is not obtained by client");
        }

    }
}
