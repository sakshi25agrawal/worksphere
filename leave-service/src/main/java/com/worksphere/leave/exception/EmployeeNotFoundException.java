package com.worksphere.leave.exception;

import com.worksphere.common.exception.ResourceNotFoundException;

public class EmployeeNotFoundException extends ResourceNotFoundException {

    public EmployeeNotFoundException(Long employeeId) {
        super(
                "Employee",
                "id",
                employeeId
        );
    }
}