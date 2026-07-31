package com.worksphere.payroll.service.impl;

import com.worksphere.common.exception.ResourceNotFoundException;
import com.worksphere.payroll.dto.PayrollResponse;
import com.worksphere.payroll.service.PayrollService;
import org.springframework.stereotype.Service;

@Service
public class PayrollServiceImpl implements PayrollService {

    @Override
    public PayrollResponse getPayrollByEmployeeId(Long employeeId) {

        if (employeeId == 101L) {

            return new PayrollResponse(
                    1L,
                    101L,
                    50000.0,
                    5000.0,
                    7000.0,
                    48000.0
            );

        }

        if (employeeId == 102L) {

            return new PayrollResponse(
                    2L,
                    102L,
                    70000.0,
                    8000.0,
                    9000.0,
                    69000.0
            );

        }

        throw new ResourceNotFoundException(
                "Payroll",
                "employeeId",
                employeeId
        );

    }

}