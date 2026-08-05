package com.worksphere.employee.mapper;

import com.worksphere.employee.dto.request.EmployeeRequest;
import com.worksphere.employee.dto.response.EmployeeResponse;
import com.worksphere.employee.entity.Employee;

public class EmployeeMapper {

    private EmployeeMapper() {
    }

    public static EmployeeResponse toResponse(Employee employee) {

        return new EmployeeResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getSalary(),
                employee.getDepartmentId()
        );
    }

    public static Employee toEntity(EmployeeRequest request) {

        Employee employee = new Employee();

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setSalary(request.salary());
        employee.setDepartmentId(request.departmentId());

        return employee;
    }

    public static void updateEntity(Employee employee,
                                    EmployeeRequest request) {

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setSalary(request.salary());
        employee.setDepartmentId(request.departmentId());
    }
}