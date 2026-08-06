package com.worksphere.employee.orchestrator;

import com.worksphere.employee.client.PayrollFeignClient;
import com.worksphere.employee.dto.external.PayrollResponse;
import com.worksphere.employee.resilience.PayrollResilienceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class PayrollAsyncService {

    private static final Logger log =
            LoggerFactory.getLogger(DepartmentAsyncService.class);

    private final PayrollResilienceService payrollResilienceService;

    @Qualifier("orchestrationExecutor")
    private final Executor orchestrationExecutor;

    public PayrollAsyncService(
             PayrollResilienceService payrollResilienceService,
            @Qualifier("orchestrationExecutor") Executor orchestrationExecutor) {
        this.payrollResilienceService = payrollResilienceService;


        this.orchestrationExecutor = orchestrationExecutor;
    }


    public CompletableFuture<PayrollResponse> getPayrollAsync(Long employeeId) {

        return CompletableFuture.supplyAsync(() -> {


            PayrollResponse response =
                    payrollResilienceService.getPayroll(employeeId);

            log.info("Executing Payroll Service on thread : {}",
                    Thread.currentThread().getName());


            return response;

        }, orchestrationExecutor);
    }
}
