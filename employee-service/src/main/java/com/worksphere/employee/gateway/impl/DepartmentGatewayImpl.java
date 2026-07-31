package com.worksphere.employee.gateway.impl;

import com.worksphere.employee.dto.DepartmentResponse;
import com.worksphere.employee.gateway.DepartmentGateway;
import com.worksphere.employee.resilience.DepartmentResilienceService;

import org.springframework.stereotype.Service;

@Service

public class DepartmentGatewayImpl implements DepartmentGateway {

    private final DepartmentResilienceService departmentResilienceService;

    public DepartmentGatewayImpl(DepartmentResilienceService departmentResilienceService) {
        this.departmentResilienceService = departmentResilienceService;
    }

    @Override
    public DepartmentResponse getDepartment(Long departmentId) {

        return departmentResilienceService.getDepartment(departmentId);

    }
}