package com.worksphere.leave.exception;

import com.worksphere.common.exception.ResourceNotFoundException;

public class LeaveTypeNotFoundException extends ResourceNotFoundException {

    public LeaveTypeNotFoundException(Long leaveTypeId) {
        super(
                "Leave Type",
                "id",
                leaveTypeId
        );
    }
}