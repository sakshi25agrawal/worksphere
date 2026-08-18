package com.worksphere.leave.repository;

import com.worksphere.leave.entity.LeaveTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveTypeEntity, Long> {

    Optional<LeaveTypeEntity> findByCode(String code);

    List<LeaveTypeEntity> findByActiveTrue();

    boolean existsByCode(String code);
}