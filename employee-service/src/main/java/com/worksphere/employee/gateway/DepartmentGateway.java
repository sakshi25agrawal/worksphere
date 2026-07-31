package com.worksphere.employee.gateway;

import com.worksphere.employee.dto.DepartmentResponse;

public interface DepartmentGateway {

    DepartmentResponse getDepartment(Long departmentId);

}