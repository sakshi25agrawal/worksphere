package com.worksphere.leave.service.impl;

import com.worksphere.leave.dto.LeaveBalanceResponseDto;
import com.worksphere.leave.entity.LeaveBalanceEntity;
import com.worksphere.leave.entity.LeaveTypeEntity;
import com.worksphere.leave.repository.LeaveBalanceRepository;
import com.worksphere.leave.repository.LeaveTypeRepository;
import com.worksphere.leave.service.LeaveBalanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LeaveBalanceServiceImpl implements LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    public LeaveBalanceServiceImpl(
            LeaveBalanceRepository leaveBalanceRepository,
            LeaveTypeRepository leaveTypeRepository) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponseDto> getEmployeeBalances(
            Long employeeId,
            Integer year) {

        return leaveBalanceRepository
                .findByEmployeeIdAndYear(employeeId, year)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void initializeEmployeeBalances(Long employeeId) {

        int currentYear = java.time.Year.now().getValue();

        List<LeaveTypeEntity> activeLeaveTypes =
                leaveTypeRepository.findByActiveTrue();

        for (LeaveTypeEntity leaveType : activeLeaveTypes) {

            boolean exists =
                    leaveBalanceRepository
                            .existsByEmployeeIdAndLeaveTypeIdAndYear(
                                    employeeId,
                                    leaveType.getId(),
                                    currentYear
                            );

            if (exists) {
                continue;
            }

            LeaveBalanceEntity balance = LeaveBalanceEntity.builder()
                    .employeeId(employeeId)
                    .leaveType(leaveType)
                    .allocatedDays(leaveType.getAnnualAllocation())
                    .usedDays(0)
                    .remainingDays(leaveType.getAnnualAllocation())
                    .year(currentYear)
                    .build();

            leaveBalanceRepository.save(balance);
        }
    }

    private LeaveBalanceResponseDto toResponse(
            LeaveBalanceEntity entity) {

        return new LeaveBalanceResponseDto(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getLeaveType().getId(),
                entity.getLeaveType().getCode(),
                entity.getLeaveType().getName(),
                entity.getAllocatedDays(),
                entity.getUsedDays(),
                entity.getRemainingDays(),
                entity.getYear()
        );
    }
}