package com.example.bankService.service;

import com.example.bankService.model.Client;

public interface ValidationService {
    boolean isClientWantedByPolice (Client client);
    boolean isClientInBlackList(Client  client);
    boolean isValidPassport(Client client);
    
}
