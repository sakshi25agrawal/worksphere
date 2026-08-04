package com.worksphere.payroll.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdatePayrollRequest(

        @NotNull(message = "Basic salary is mandatory")
        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal basicSalary,

        @NotNull(message = "Bonus is mandatory")
        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal bonus,

        @NotNull(message = "Tax is mandatory")
        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal tax

) {
}