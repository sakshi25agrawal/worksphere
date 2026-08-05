package com.worksphere.employee.dto.response;

import com.worksphere.employee.dto.external.DepartmentResponse;
import com.worksphere.employee.dto.external.PayrollResponse;

public record EmployeeDetailsResponse(

        EmployeeResponse employee,

        DepartmentResponse department,

        PayrollResponse payroll

) {
}