package com.worksphere.kafka.event;

public record EmployeeCreatedEvent(
        Long employeeId,
        String firstName,
        String lastName,
        String email,
        Double salary,
        Long departmentId
) {
}