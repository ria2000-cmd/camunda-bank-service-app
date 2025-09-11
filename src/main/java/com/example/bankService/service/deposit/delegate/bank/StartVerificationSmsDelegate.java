package com.example.bankService.service.deposit.delegate.bank;

import com.example.bankService.model.Client;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.bankService.util.Constants.RIA;

@Slf4j
@Component("startVerificationSmsDelegate")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StartVerificationSmsDelegate implements JavaDelegate {

   public static final  String START_MESSAGE = "message_start_sms_verification";

    // we need the runtimeService for messages
    RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        log.info("The StartVerificationSmsDelegate has started....");

        var businessKey = delegateExecution.getBusinessKey();
        //needs to investigate why the client here is null, hence why I hard coded Ria instead of getting here from execution
//        var client = (Client) delegateExecution.getVariable("client");
        var client = RIA;

        runtimeService.createMessageCorrelation(START_MESSAGE)
                .processInstanceBusinessKey(businessKey)
                .setVariable("client", client)
                .correlateWithResult();

    }
}
