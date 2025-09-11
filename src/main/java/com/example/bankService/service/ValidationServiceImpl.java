package com.example.bankService.service;

import com.example.bankService.model.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static com.example.bankService.util.Constants.BANK_BLACK_LIST;
import static com.example.bankService.util.Constants.POLICE_WANTED_LIST;

@Slf4j
@Service
public class ValidationServiceImpl implements ValidationService{
    @Override
    public boolean isClientWantedByPolice(Client client) {
        return POLICE_WANTED_LIST.stream()
                .anyMatch(info -> matchesClientInfo(info, client));

    }

    @Override
    public boolean isClientInBlackList(Client client) {
        return BANK_BLACK_LIST.stream()
                .anyMatch(info -> matchesClientInfo(info, client));
    }

    @Override
    public boolean isValidPassport(Client client) {
        var passport =  client.getPassport();
        return passport.getValidFrom().isBefore(LocalDate.now())
                && passport.getValidTo().isAfter(LocalDate.now());
    }

    private boolean matchesClientInfo(Client info, Client client){
        return info.getName().equals(client.getName())
                && info.getSurname().equals(client.getSurname())
                && info.getBirthDate().equals(client.getBirthDate())
                && info.getPassport().getIdenticalNumber().equals(client.getPassport().getIdenticalNumber());
    }


}
