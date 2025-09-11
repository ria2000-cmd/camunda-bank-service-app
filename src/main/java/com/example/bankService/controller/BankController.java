package com.example.bankService.controller;

import com.example.bankService.model.Client;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.ProcessEngines;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import static com.example.bankService.util.Constants.*;

@RestController
@Slf4j
@RequestMapping("/bank")
public class BankController {

    @PostMapping("/start/{businessKey}")
    public ResponseEntity<String> startBankProcess(@PathVariable("businessKey") String businessKey) {
        log.info(String.format("Start banking process with business key id:  %S",businessKey ));

        if (StringUtils.isEmpty(businessKey)) {
            return ResponseEntity.badRequest()
                    .body("Business key can not be empty or null");
        }

        ProcessEngines.getDefaultProcessEngine()
                .getRuntimeService()
                .createProcessInstanceByKey(MAIN_DEPOSIT_CREDIT_PROCESS)
                .businessKey(businessKey)
                .setVariables(prepareVariables(RIA))
                .executeWithVariablesInReturn();

        return ResponseEntity.ok()
                .body(String.format("Banking process with business key: %s - has started", businessKey));
    }

    private Map<String, Object> prepareVariables(Client client){

        var variableMap = new HashMap<String, Object>();
        variableMap.put("client", client);

        return variableMap;
    }

}
