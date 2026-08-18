package com.worksphere.leave.dto;

public record LeaveBalanceResponseDto(

        Long id,

        Long employeeId,

        Long leaveTypeId,

        String leaveTypeCode,

        String leaveTypeName,

        Integer allocatedDays,

        Integer usedDays,

        Integer remainingDays,

        Integer year
) {
}