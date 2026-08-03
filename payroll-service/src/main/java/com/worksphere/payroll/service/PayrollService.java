package com.worksphere.payroll.service;

import com.worksphere.payroll.dto.request.CreatePayrollRequest;
import com.worksphere.payroll.dto.response.PayrollResponse;

public interface PayrollService {


    PayrollResponse createPayroll(CreatePayrollRequest request);

    PayrollResponse getPayrollByEmployeeId(Long employeeId);

}