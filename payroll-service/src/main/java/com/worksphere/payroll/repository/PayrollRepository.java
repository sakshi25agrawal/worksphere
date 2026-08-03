package com.worksphere.payroll.repository;

import com.worksphere.payroll.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    Optional<Payroll> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeId(Long employeeId);

}