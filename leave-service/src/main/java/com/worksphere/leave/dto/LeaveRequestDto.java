package com.worksphere.leave.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record LeaveRequestDto(

        @NotNull(message = "Employee ID is required")
        Long employeeId,

        @NotNull(message = "Leave type is required")
        Long leaveTypeId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @NotBlank(message = "Reason is required")
        @Size(
                max = 500,
                message = "Reason must not exceed 500 characters"
        )
        String reason
) {
}