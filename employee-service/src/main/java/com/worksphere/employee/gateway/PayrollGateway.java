package com.worksphere.employee.gateway;

import com.worksphere.employee.dto.external.PayrollResponse;

public interface PayrollGateway {

    PayrollResponse getPayrollByEmployeeId(Long employeeId);

}