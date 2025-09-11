package com.example.bankService.service.deposit.delegate.bank;

import com.example.bankService.model.Client;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;

import static com.example.bankService.util.Constants.RIA;

@Slf4j
@Component("prepareSmsDelegate")
public class PrepareSmsDelegate implements JavaDelegate {


    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        log.info("the prepareSmsDelegate has started.....");

        //needs to investigate why the client here is null, hence why I hard coded Ria instead of getting here from execution
//        var client = (Client) delegateExecution.getVariable("client");
        var client = RIA;

        log.info(String.format("Preparation for the SMS sending to tel.number: %s", client.getPhoneNumber()));
        var code = prepareSmsCode();

        log.info("Sending verification mobile code to client..........");

        delegateExecution.setVariable("sendMobileCode",code);

        var sendMobileCodeCount = (Integer) delegateExecution.getVariable("sendMobileCodeCount");

        Optional.ofNullable(sendMobileCodeCount)
                .ifPresentOrElse(
                        (count) -> delegateExecution.setVariable("sendMobileCodeCount", count + 1 ),
                        () ->delegateExecution.setVariable("sendMobileCodeCount", 1));
    }

    private  int prepareSmsCode (){
        return new Random().nextInt(1_000_000);
    }
}
