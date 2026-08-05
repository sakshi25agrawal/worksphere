package com.worksphere.employee.dto;

public record DepartmentResponse(

        Long id,
        String departmentName,
        String departmentCode,
        String departmentHead,
        String location

) {
}