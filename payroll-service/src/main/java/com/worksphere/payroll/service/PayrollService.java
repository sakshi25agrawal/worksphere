package com.worksphere.payroll.service;

import com.worksphere.payroll.dto.request.CreatePayrollRequest;
import com.worksphere.payroll.dto.response.PayrollResponse;

import java.util.List;

public interface PayrollService {


    PayrollResponse createPayroll(CreatePayrollRequest request);

    PayrollResponse getPayrollByEmployeeId(Long employeeId);

    List<PayrollResponse> getAllPayrolls();
}