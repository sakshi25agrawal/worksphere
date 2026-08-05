package com.worksphere.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EmployeeRequest(

        @Schema(
                description = "Employee First Name",
                example = "Sakshi"
        )
        @NotBlank(message = "First name is required")
        String firstName,

        @Schema(
                description = "Employee Last Name",
                example = "Agrawal"
        )
        @NotBlank(message = "Last name is required")
        String lastName,


        @Schema(
                description = "Employee Email",
                example = "sakshi@gmail.com"
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email")
        String email,

        @Schema(
                description = "Employee Salary",
                example = "65000"
        )
        @Positive(message = "Salary must be greater than zero")
        Double salary,

        @Schema(example = "1")
        @NotNull(message = "Department Id is required")
        Long departmentId

) {



}