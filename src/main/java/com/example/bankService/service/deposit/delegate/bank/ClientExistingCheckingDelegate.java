package com.example.bankService.service.deposit.delegate.bank;

import com.example.bankService.model.Client;
import com.example.bankService.model.Passport;
import lombok.extern.slf4j.Slf4j;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static com.example.bankService.util.Constants.BANK_ALREADY_CLIENTS_INFO;
import static com.example.bankService.util.Constants.RIA;


@Slf4j
@Component("clientExistingCheckingDelegate")
public class ClientExistingCheckingDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        log.info("The ClientExistingCheckingDelegate has started....");
        boolean isExistingUser;

        //needs to investigate why the client here is null, hence why I hard coded Ria instead of getting here from execution
//        var client = (Client) delegateExecution.getVariable("client");
        var client = RIA;
        var passport = client.getPassport();

        //todo obtain the list of bankClientsInfo from DB

        isExistingUser = BANK_ALREADY_CLIENTS_INFO.stream()
                .anyMatch(info -> matchesClientInfo(info, passport));

        if(isExistingUser){
            log.info(String.format("thd user with name: %s is already client in our bank ",client.getName()));
        }else{
            log.info(String.format("thd user with name: %s is not client of our bank ",client.getName()));
        }

        delegateExecution.setVariable("isExistingUser", isExistingUser);

    }

    private boolean matchesClientInfo(Passport info, Passport passport){
       return info.getIdenticalNumber().equals(passport.getIdenticalNumber())
               && info.getName().equals(passport.getName())
               && info.getSurname().equals(passport.getSurname())
               && info.getBirthDate().equals(passport.getBirthDate());
    }


}
