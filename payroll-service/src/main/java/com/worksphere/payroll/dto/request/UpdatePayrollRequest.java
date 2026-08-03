package com.worksphere.payroll.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdatePayrollRequest(

        @NotNull
        @DecimalMin("0.0")
        BigDecimal basicSalary,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal bonus,

        @NotNull
        @DecimalMin("0.0")
        BigDecimal tax

) {
}