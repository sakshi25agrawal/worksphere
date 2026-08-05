package com.worksphere.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employee with Department Details")
public record EmployeeWithDepartmentResponse(

        EmployeeResponse employee,

        DepartmentResponse department

) {}