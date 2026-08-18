package com.worksphere.leave.service;

import com.worksphere.leave.dto.LeaveRejectRequestDto;
import com.worksphere.leave.dto.LeaveRequestDto;
import com.worksphere.leave.dto.LeaveResponseDto;

import java.util.List;

public interface LeaveService {

    LeaveResponseDto applyLeave(LeaveRequestDto request);

    LeaveResponseDto getLeaveById(Long leaveId);

    List<LeaveResponseDto> getLeavesByEmployee(Long employeeId);

    LeaveResponseDto approveLeave(Long leaveId, Long approverId);

    LeaveResponseDto rejectLeave(
            Long leaveId,
            LeaveRejectRequestDto request
    );

    LeaveResponseDto cancelLeave(Long leaveId);
}