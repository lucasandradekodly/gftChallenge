package com.db.awmd.challenge.dto;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class TransferDto {

    @NotEmpty(message = "Account from must not be null")
    private String accountFrom;

    @NotEmpty(message = "Account to must not be null")
    private String accountTo;

    @DecimalMin(value = "0.0", inclusive = false, message = "The value must be positive")
    private BigDecimal amount;
}
