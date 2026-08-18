package com.worksphere.leave.dto;

public record LeaveTypeResponseDto(

        Long id,

        String code,

        String name,

        Integer annualAllocation,

        String description,

        Boolean active
) {
}