package com.example.bankService.service.deposit.delegate.bank;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("sendFailedVerificationSmsDelegate")
public class SendFailedVerificationSmsDelegate implements JavaDelegate {

    public static final  String START_FAILED_MESSAGE = "message_failed_sms_verification";

    // we need the runtimeService for messages
    RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("The sendFailedVerificationSmsDelegate has started....");

        var businessKey = delegateExecution.getBusinessKey();

        runtimeService.createMessageCorrelation(START_FAILED_MESSAGE)
                .processInstanceBusinessKey(businessKey)
                .correlateWithResult();

    }
}
