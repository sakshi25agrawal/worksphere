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

    @Resource(name = "departmentExecutor")
    private Executor departmentExecutor;

    public DepartmentAsyncService(
            DepartmentResilienceService departmentResilienceService) {
        this.departmentResilienceService = departmentResilienceService;
    }

    public CompletableFuture<DepartmentResponse> getDepartmentAsync(
            Long departmentId) {

        log.info("Submitting Department Service request to async executor");

        return CompletableFuture.supplyAsync(() -> {

            log.info("Executing Department Service call on thread : {}",
                    Thread.currentThread().getName());

            return departmentResilienceService.getDepartment(departmentId);

        }, departmentExecutor);

    }

}