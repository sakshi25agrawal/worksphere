package com.worksphere.employee.gateway.impl;

import com.worksphere.employee.client.PayrollFeignClient;
import com.worksphere.employee.dto.external.PayrollResponse;
import com.worksphere.employee.gateway.PayrollGateway;
import org.springframework.stereotype.Component;

@Component
public class PayrollGatewayImpl implements PayrollGateway {

    private final PayrollFeignClient payrollFeignClient;

    public PayrollGatewayImpl(PayrollFeignClient payrollFeignClient) {
        this.payrollFeignClient = payrollFeignClient;
    }

    @Override
    public PayrollResponse getPayrollByEmployeeId(Long employeeId) {



        return payrollFeignClient.getPayrollByEmployeeId(employeeId);
    }
}