package com.worksphere.leave.service;

import com.worksphere.leave.dto.LeaveBalanceResponseDto;

import java.util.List;

public interface LeaveBalanceService {

    List<LeaveBalanceResponseDto> getEmployeeBalances(
            Long employeeId,
            Integer year
    );
}