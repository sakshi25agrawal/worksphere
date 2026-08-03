package com.worksphere.payroll.dto.response;
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