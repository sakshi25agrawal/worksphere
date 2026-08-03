package com.worksphere.payroll.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePayrollRequest(

        @NotNull
        Long employeeId,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal basicSalary,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal bonus,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal tax

) {
}