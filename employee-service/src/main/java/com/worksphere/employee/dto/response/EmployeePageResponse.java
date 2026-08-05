package com.worksphere.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record EmployeePageResponse(

        @Schema(description = "List of Employees")
        List<EmployeeResponse> employees,

        @Schema(example = "0")
        int pageNumber,

        @Schema(example = "5")
        int pageSize,

        @Schema(example = "25")
        long totalElements,

        @Schema(example = "5")
        int totalPages,

        @Schema(example = "false")
        boolean last

) {
}