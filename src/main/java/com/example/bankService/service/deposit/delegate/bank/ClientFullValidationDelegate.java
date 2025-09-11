package com.example.bankService.service.deposit.delegate.bank;

import com.example.bankService.model.Client;
import com.example.bankService.service.ValidationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.bankService.util.Constants.RIA;

@Slf4j
@RequiredArgsConstructor
@Component("clientFullValidationDelegate")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientFullValidationDelegate implements JavaDelegate {

    ValidationService validationService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        log.info("The ClientFullValidationDelegate has started....");

        //needs to investigate why the client here is null, hence why I hard coded Ria instead of getting here from execution
//        var client = (Client) delegateExecution.getVariable("client");
        var client = RIA;

        var isCriminal = validationService.isClientWantedByPolice(client);
        var isInBlackList = validationService.isClientInBlackList(client);
        var isValidPassport = validationService.isValidPassport(client);

        delegateExecution.setVariable("isCriminal", isCriminal);

        if(isCriminal || isInBlackList || !isValidPassport){
            delegateExecution.setVariable("isValidUser", false);
        }else {
            delegateExecution.setVariable("isValidUser", true);
        }

    }
}
