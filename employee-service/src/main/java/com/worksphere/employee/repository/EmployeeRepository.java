package com.worksphere.employee.repository;

import com.worksphere.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {


    boolean existsByEmail(String email);
}