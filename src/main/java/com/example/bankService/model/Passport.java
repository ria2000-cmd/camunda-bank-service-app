package com.example.bankService.model;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Passport implements Serializable {

    String identicalNumber;
    String name;
    String surname;
    String address;
    LocalDate birthDate;
    LocalDate validFrom;
    LocalDate validTo;

}
