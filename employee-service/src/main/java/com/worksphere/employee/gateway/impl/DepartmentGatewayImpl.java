package com.worksphere.employee.gateway.impl;

import com.worksphere.employee.dto.DepartmentResponse;
import com.worksphere.employee.gateway.DepartmentGateway;
import com.worksphere.employee.orchestrator.DepartmentAsyncService;
import com.worksphere.employee.resilience.DepartmentResilienceService;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class DepartmentGatewayImpl implements DepartmentGateway {

    private final DepartmentResilienceService departmentResilienceService;

    private final DepartmentAsyncService departmentAsyncService;

    public DepartmentGatewayImpl(
            DepartmentResilienceService departmentResilienceService,
            DepartmentAsyncService departmentAsyncService) {

        this.departmentResilienceService = departmentResilienceService;
        this.departmentAsyncService = departmentAsyncService;
    }

    @Override
    public DepartmentResponse getDepartment(Long departmentId) {

        return departmentResilienceService.getDepartment(departmentId);

    }

    @Override
    public CompletableFuture<DepartmentResponse> getDepartmentAsync(
            Long departmentId) {

        return departmentAsyncService.getDepartmentAsync(departmentId);

    }

}