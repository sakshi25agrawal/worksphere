package com.worksphere.payroll.service.impl;

import com.worksphere.common.exception.ResourceNotFoundException;
import com.worksphere.payroll.dto.response.PayrollResponse;
import com.worksphere.payroll.service.PayrollService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PayrollServiceImpl implements PayrollService {

    @Override
    public PayrollResponse getPayrollByEmployeeId(Long employeeId) {

        if (employeeId == 101L) {

            return new PayrollResponse(
                    1L,
                    101L,
                    BigDecimal.valueOf(50000),
                    BigDecimal.valueOf(5000),
                    BigDecimal.valueOf(7000),
                    BigDecimal.valueOf(48000)
            );

        }

        if (employeeId == 102L) {

            return new PayrollResponse(
                    2L,
                    102L,
                    BigDecimal.valueOf(70000.0),
                    BigDecimal.valueOf(8000.0),
                    BigDecimal.valueOf(9000.0),
                    BigDecimal.valueOf(69000.0)
            );

        }

        throw new ResourceNotFoundException(
                "Payroll",
                "employeeId",
                employeeId
        );

    }

}