package com.worksphere.employee.gateway;

import com.worksphere.employee.dto.external.PayrollResponse;

import java.util.concurrent.CompletableFuture;

public interface PayrollGateway {

    PayrollResponse getPayrollByEmployeeId(Long employeeId);

    CompletableFuture<PayrollResponse> getPayrollAsync(Long employeeId);
}