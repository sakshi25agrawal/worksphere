package com.worksphere.leave.dto.client;

public record EmployeeResponse(

        Long id,

        String firstName,

        String lastName,

        String email,

        Double salary,

        Long departmentId
) {
}