package com.example.bankService.model;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepositContract {

    UUID id;
    String name;
    BigDecimal minimalSum;
    OffsetDateTime openDate;
    OffsetDateTime closeDate;
    String clientName;
    String clientSurName;
    String clientPhoneNumber;
}
