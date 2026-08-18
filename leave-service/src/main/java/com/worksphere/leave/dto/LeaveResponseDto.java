package com.worksphere.leave.dto;

import com.worksphere.leave.enums.LeaveStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveResponseDto(

        Long id,

        Long employeeId,

        Long leaveTypeId,

        String leaveTypeCode,

        String leaveTypeName,

        LocalDate startDate,

        LocalDate endDate,

        Integer numberOfDays,

        String reason,

        LeaveStatus status,

        LocalDateTime appliedAt,

        LocalDateTime approvedAt,

        LocalDateTime rejectedAt,

        LocalDateTime cancelledAt,

        Long approverId,

        String rejectionReason,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}