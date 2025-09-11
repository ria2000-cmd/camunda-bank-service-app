package com.example.bankService.service.deposit.delegate.bank;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

import static com.example.bankService.util.Constants.LIMIT_OF_VERIFICATION_SMS_ATTEMPTS_EXCEEDED;

@Slf4j
@Component("smsValidationDelegate")
public class SmsValidationDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("The SmsValidationDelegate has started....");

        var obtainedMobileCode = (Integer) delegateExecution.getVariable("obtainedMobileCode");
        var sendMobileCode = (Integer) delegateExecution.getVariable("sendMobileCode");


        if (ObjectUtils.anyNull(obtainedMobileCode, sendMobileCode)) {
            throw new IllegalArgumentException(String.format("One of the argument is null : %s , %s ", obtainedMobileCode, sendMobileCode));
        }

        if(Objects.equals(obtainedMobileCode, sendMobileCode)){
            delegateExecution.setVariable("isSmsCodeValid", true);
        }else {
            var sendMobileCount = (Integer) Optional.ofNullable(delegateExecution.getVariable("sendMobileCount")).orElse(1);

            if(sendMobileCount == 3){
                throw new BpmnError(LIMIT_OF_VERIFICATION_SMS_ATTEMPTS_EXCEEDED, "The count of chances to verify mobile code is greater than allowed!");
            }
            delegateExecution.setVariable("isSmsCodeValid", false);

            log.info("The verification sms code  does not match the sent one....");


        }
    }

}
