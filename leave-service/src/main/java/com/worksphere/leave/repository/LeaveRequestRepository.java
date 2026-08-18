package com.worksphere.leave.repository;

import com.worksphere.leave.entity.LeaveRequestEntity;
import com.worksphere.leave.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequestEntity, Long> {

    List<LeaveRequestEntity> findByEmployeeIdOrderByCreatedAtDesc(
            Long employeeId
    );

    List<LeaveRequestEntity> findByEmployeeIdAndStatusOrderByCreatedAtDesc(
            Long employeeId,
            LeaveStatus status
    );

    @Query("""
            SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END
            FROM LeaveRequestEntity l
            WHERE l.employeeId = :employeeId
              AND l.status IN :statuses
              AND l.startDate <= :endDate
              AND l.endDate >= :startDate
            """)
    boolean existsOverlappingLeave(
            @Param("employeeId") Long employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<LeaveStatus> statuses
    );
}