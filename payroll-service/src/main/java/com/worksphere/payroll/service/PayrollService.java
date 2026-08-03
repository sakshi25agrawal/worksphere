package com.worksphere.payroll.service;

import com.worksphere.payroll.dto.response.PayrollResponse;

public interface PayrollService {

    PayrollResponse getPayrollByEmployeeId(Long employeeId);

}