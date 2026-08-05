package com.worksphere.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmployeeResponse(

        @Schema(description = "Employee ID", example = "1")
        Long id,

        @Schema(description = "First Name", example = "Sakshi")
        String firstName,

        @Schema(description = "Last Name", example = "Agrawal")
        String lastName,

        @Schema(description = "Email Address", example = "sakshi@gmail.com")
        String email,

        @Schema(description = "Salary", example = "65000")
        Double salary,

        Long departmentId
) {
}