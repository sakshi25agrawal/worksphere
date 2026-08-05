package com.worksphere.employee.gateway;

import com.worksphere.employee.dto.external.DepartmentResponse;

import java.util.concurrent.CompletableFuture;

public interface DepartmentGateway {

    DepartmentResponse getDepartmentById(Long departmentId);

    //For Async
    CompletableFuture<DepartmentResponse>
    getDepartmentAsync(Long departmentId);
}