package com.worksphere.employee.dto.external;

public record DepartmentResponse(

        Long id,
        String departmentName,
        String departmentCode,
        String departmentHead,
        String location

) {
}