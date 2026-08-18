package com.worksphere.leave.exception;

import com.worksphere.common.exception.ResourceNotFoundException;

public class LeaveNotFoundException extends ResourceNotFoundException {

    public LeaveNotFoundException(Long leaveId) {
        super(
                "Leave Request",
                "id",
                leaveId
        );
    }
}