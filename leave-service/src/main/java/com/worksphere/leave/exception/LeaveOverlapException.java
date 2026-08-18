package com.worksphere.leave.exception;

public class LeaveOverlapException extends RuntimeException {

    public LeaveOverlapException(Long employeeId) {
        super(
                "Leave request overlaps with an existing leave "
                        + "for employee: "
                        + employeeId
        );
    }
}