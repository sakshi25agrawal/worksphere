package com.worksphere.leave.exception;

import com.worksphere.leave.enums.LeaveStatus;

public class InvalidLeaveStateException
        extends RuntimeException {

    public InvalidLeaveStateException(
            Long leaveId,
            LeaveStatus currentStatus,
            String operation) {

        super(
                "Cannot "
                        + operation
                        + " leave "
                        + leaveId
                        + " because current status is "
                        + currentStatus
        );
    }
}