package com.worksphere.employee.dto.external;

import java.math.BigDecimal;

public record PayrollResponse(

        Long id,
        Long employeeId,
        BigDecimal basicSalary,
        BigDecimal bonus,
        BigDecimal tax,
        BigDecimal netSalary

) {
}