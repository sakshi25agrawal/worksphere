package com.worksphere.employee.orchestrator;

import com.worksphere.employee.dto.external.DepartmentResponse;
import com.worksphere.employee.resilience.DepartmentResilienceService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class DepartmentAsyncService {

    private static final Logger log =
            LoggerFactory.getLogger(DepartmentAsyncService.class);

    private final DepartmentResilienceService departmentResilienceService;

    @Resource(name = "orchestrationExecutor")
    private final  Executor orchestrationExecutor;

    public DepartmentAsyncService(
            DepartmentResilienceService departmentResilienceService, Executor orchestrationExecutor) {
        this.departmentResilienceService = departmentResilienceService;
        this.orchestrationExecutor = orchestrationExecutor;
    }
    public CompletableFuture<DepartmentResponse> getDepartmentAsync(Long departmentId) {

        log.info("Submitting Department Service request to async executor");
        return CompletableFuture.supplyAsync(() -> {


            DepartmentResponse response =
                    departmentResilienceService.getDepartment(departmentId);
            log.info("Executing Department Service on thread : {}",
                    Thread.currentThread().getName());
            return response;

        }, orchestrationExecutor);
    }

}