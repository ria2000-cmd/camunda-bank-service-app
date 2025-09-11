package com.example.bankService.service.deposit.delegate.bank;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component("sendSuccessVerificationSmsDelegate")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendSuccessVerificationSmsDelegate implements JavaDelegate {

    public static final  String START_SUCCESS_MESSAGE = "message_success_sms_verification";

    // we need the runtimeService for messages
    RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        log.info("The SendSuccessVerificationSmsDelegate has started....");

        var businessKey = delegateExecution.getBusinessKey();

        runtimeService.createMessageCorrelation(START_SUCCESS_MESSAGE)
                .processInstanceBusinessKey(businessKey)
                .correlateWithResult();

    }
}
