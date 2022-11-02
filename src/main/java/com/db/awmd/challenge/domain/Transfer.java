package com.db.awmd.challenge.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Generated;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class Transfer {

    @Generated
    private String id;

    @NotNull
    private Account accountFrom;

    @NotNull
    private Account accountTo;

    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @NotNull
    private TransferState state;

    private Date date;
}
