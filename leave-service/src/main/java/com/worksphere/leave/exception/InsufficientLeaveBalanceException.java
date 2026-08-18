package com.worksphere.leave.exception;

public class InsufficientLeaveBalanceException
        extends RuntimeException {

    public InsufficientLeaveBalanceException(
            Long employeeId,
            Long leaveTypeId) {

        super(
                "Insufficient leave balance for employee "
                        + employeeId
                        + " and leave type "
                        + leaveTypeId
        );
    }
}