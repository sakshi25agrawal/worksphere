package com.worksphere.employee.dto.response;

import com.worksphere.employee.dto.external.DepartmentResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employee with Department Details")
public record EmployeeWithDepartmentResponse(

        EmployeeResponse employee,

        DepartmentResponse department

) {}