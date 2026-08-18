package com.worksphere.leave.repository;

import com.worksphere.leave.entity.LeaveBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalanceEntity, Long> {

    Optional<LeaveBalanceEntity> findByEmployeeIdAndLeaveTypeIdAndYear(
            Long employeeId,
            Long leaveTypeId,
            Integer year
    );

    List<LeaveBalanceEntity> findByEmployeeIdAndYear(
            Long employeeId,
            Integer year
    );

    boolean existsByEmployeeIdAndLeaveTypeIdAndYear(
            Long employeeId,
            Long leaveTypeId,
            Integer year
    );
}