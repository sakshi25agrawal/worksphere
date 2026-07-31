package com.worksphere.employee.gateway;

import com.worksphere.employee.dto.DepartmentResponse;

import java.util.concurrent.CompletableFuture;

public interface DepartmentGateway {

    DepartmentResponse getDepartment(Long departmentId);

    //For Async
    CompletableFuture<DepartmentResponse>
    getDepartmentAsync(Long departmentId);
}