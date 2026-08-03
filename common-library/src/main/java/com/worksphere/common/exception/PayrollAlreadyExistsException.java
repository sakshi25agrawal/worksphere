package com.worksphere.common.exception;

public class PayrollAlreadyExistsException extends RuntimeException {

    public PayrollAlreadyExistsException(Long employeeId) {
        super("Payroll already exists for employee id : " + employeeId);
    }

}