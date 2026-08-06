package com.worksphere.employee.gateway.impl;

import com.worksphere.employee.client.PayrollFeignClient;
import com.worksphere.employee.dto.external.PayrollResponse;
import com.worksphere.employee.gateway.PayrollGateway;
import com.worksphere.employee.orchestrator.PayrollAsyncService;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.stereotype.Component;


@Component
public class PayrollGatewayImpl implements PayrollGateway {

    private final PayrollFeignClient payrollFeignClient;

    private final Executor orchestrationExecutor;
    private final PayrollAsyncService payrollAsyncService;
    public PayrollGatewayImpl(
            PayrollFeignClient payrollFeignClient,
            @Qualifier("orchestrationExecutor") Executor orchestrationExecutor, PayrollAsyncService payrollAsyncService) {

        this.payrollFeignClient = payrollFeignClient;
        this.orchestrationExecutor = orchestrationExecutor;
        this.payrollAsyncService = payrollAsyncService;
    }
    @Override
    public PayrollResponse getPayrollByEmployeeId(Long employeeId) {

        return payrollFeignClient.getPayrollByEmployeeId(employeeId);
    }
    @Override
    public CompletableFuture<PayrollResponse> getPayrollAsync(Long employeeId) {

        return payrollAsyncService.getPayrollAsync(employeeId);
    }
}