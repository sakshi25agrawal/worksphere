package com.worksphere.leave.service.impl;

import com.worksphere.leave.client.EmployeeFeignClient;
import com.worksphere.leave.dto.LeaveRequestDto;
import com.worksphere.leave.dto.LeaveResponseDto;
import com.worksphere.leave.dto.LeaveRejectRequestDto;
import com.worksphere.leave.entity.LeaveRequestEntity;
import com.worksphere.leave.entity.LeaveTypeEntity;
import com.worksphere.leave.enums.LeaveStatus;
import com.worksphere.leave.exception.*;
import com.worksphere.leave.mapper.LeaveMapper;
import com.worksphere.leave.repository.LeaveBalanceRepository;
import com.worksphere.leave.repository.LeaveRequestRepository;
import com.worksphere.leave.repository.LeaveTypeRepository;
import com.worksphere.leave.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeFeignClient employeeFeignClient;
    private final LeaveMapper leaveMapper;

    @Override
    public LeaveResponseDto applyLeave(LeaveRequestDto request) {

        validateDates(request.startDate(), request.endDate());

        validateEmployee(request.employeeId());

        LeaveTypeEntity leaveType = leaveTypeRepository
                .findById(request.leaveTypeId())
                .orElseThrow(() ->
                        new LeaveTypeNotFoundException(
                                request.leaveTypeId()
                        )
                );

        if (!Boolean.TRUE.equals(leaveType.getActive())) {
            throw new IllegalStateException(
                    "Leave type is inactive: " + leaveType.getCode()
            );
        }

        int numberOfDays = calculateLeaveDays(
                request.startDate(),
                request.endDate()
        );

        validateNoOverlappingLeave(
                request.employeeId(),
                request.startDate(),
                request.endDate()
        );

        validateLeaveBalance(
                request.employeeId(),
                request.leaveTypeId(),
                numberOfDays
        );

        LeaveRequestEntity entity =
                leaveMapper.toEntity(request);

        entity.setLeaveType(leaveType);
        entity.setNumberOfDays(numberOfDays);
        entity.setStatus(LeaveStatus.APPLIED);
        entity.setAppliedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        LeaveRequestEntity saved =
                leaveRequestRepository.save(entity);

        return leaveMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveResponseDto getLeaveById(Long leaveId) {

        LeaveRequestEntity entity =
                leaveRequestRepository.findById(leaveId)
                        .orElseThrow(() ->
                                new LeaveNotFoundException(leaveId)
                        );

        return leaveMapper.toResponseDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getLeavesByEmployee(
            Long employeeId) {

        validateEmployee(employeeId);

        return leaveRequestRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(leaveMapper::toResponseDto)
                .toList();
    }

    @Override
    public LeaveResponseDto approveLeave(
            Long leaveId,
            Long approverId) {

        LeaveRequestEntity leave = leaveRequestRepository
                .findById(leaveId)
                .orElseThrow(() ->
                        new LeaveNotFoundException(leaveId)
                );

        if (leave.getStatus() != LeaveStatus.APPLIED) {
            throw new InvalidLeaveStateException(
                    leaveId,
                    leave.getStatus(),
                    "approve"
            );
        }

        int year = leave.getStartDate().getYear();

        var balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(
                        leave.getEmployeeId(),
                        leave.getLeaveType().getId(),
                        year
                )
                .orElseThrow(() ->
                        new InsufficientLeaveBalanceException(
                                leave.getEmployeeId(),
                                leave.getLeaveType().getId()
                        )
                );

        if (balance.getRemainingDays() < leave.getNumberOfDays()) {
            throw new InsufficientLeaveBalanceException(
                    leave.getEmployeeId(),
                    leave.getLeaveType().getId()
            );
        }

        balance.setUsedDays(
                balance.getUsedDays() + leave.getNumberOfDays()
        );

        balance.setRemainingDays(
                balance.getRemainingDays()
                        - leave.getNumberOfDays()
        );

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setApproverId(approverId);
        leave.setApprovedAt(LocalDateTime.now());
        leave.setUpdatedAt(LocalDateTime.now());

        leaveBalanceRepository.save(balance);

        LeaveRequestEntity saved =
                leaveRequestRepository.save(leave);

        return leaveMapper.toResponseDto(saved);
    }

    @Override
    public LeaveResponseDto rejectLeave(
            Long leaveId,
            LeaveRejectRequestDto request) {

        LeaveRequestEntity leave = leaveRequestRepository
                .findById(leaveId)
                .orElseThrow(() ->
                        new LeaveNotFoundException(leaveId)
                );

        if (leave.getStatus() != LeaveStatus.APPLIED) {
            throw new InvalidLeaveStateException(
                    leaveId,
                    leave.getStatus(),
                    "reject"
            );
        }

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setRejectionReason(request.rejectionReason());
        leave.setApproverId(request.approverId());
        leave.setRejectedAt(LocalDateTime.now());
        leave.setUpdatedAt(LocalDateTime.now());

        LeaveRequestEntity saved =
                leaveRequestRepository.save(leave);

        return leaveMapper.toResponseDto(saved);
    }

    @Override
    public LeaveResponseDto cancelLeave(Long leaveId) {

        LeaveRequestEntity leave = leaveRequestRepository
                .findById(leaveId)
                .orElseThrow(() ->
                        new LeaveNotFoundException(leaveId)
                );

        if (leave.getStatus() != LeaveStatus.APPLIED
                && leave.getStatus() != LeaveStatus.APPROVED) {

            throw new InvalidLeaveStateException(
                    leaveId,
                    leave.getStatus(),
                    "cancel"
            );
        }

        if (leave.getStatus() == LeaveStatus.APPROVED) {

            int year = leave.getStartDate().getYear();

            var balance = leaveBalanceRepository
                    .findByEmployeeIdAndLeaveTypeIdAndYear(
                            leave.getEmployeeId(),
                            leave.getLeaveType().getId(),
                            year
                    )
                    .orElseThrow(() ->
                            new InsufficientLeaveBalanceException(
                                    leave.getEmployeeId(),
                                    leave.getLeaveType().getId()
                            )
                    );

            balance.setUsedDays(
                    balance.getUsedDays()
                            - leave.getNumberOfDays()
            );

            balance.setRemainingDays(
                    balance.getRemainingDays()
                            + leave.getNumberOfDays()
            );

            leaveBalanceRepository.save(balance);
        }

        leave.setStatus(LeaveStatus.CANCELLED);
        leave.setCancelledAt(LocalDateTime.now());
        leave.setUpdatedAt(LocalDateTime.now());

        LeaveRequestEntity saved =
                leaveRequestRepository.save(leave);

        return leaveMapper.toResponseDto(saved);
    }

    private void validateEmployee(Long employeeId) {

        try {
            employeeFeignClient.getEmployeeById(employeeId);
        } catch (Exception exception) {
            throw new EmployeeNotFoundException(employeeId);
        }
    }

    private void validateDates(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date"
            );
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Leave cannot be applied for a past date"
            );
        }
    }

    private int calculateLeaveDays(
            LocalDate startDate,
            LocalDate endDate) {

        return (int) ChronoUnit.DAYS.between(
                startDate,
                endDate
        ) + 1;
    }

    private void validateNoOverlappingLeave(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate) {

        boolean overlapping =
                leaveRequestRepository.existsOverlappingLeave(
                        employeeId,
                        startDate,
                        endDate,
                        List.of(
                                LeaveStatus.APPLIED,
                                LeaveStatus.APPROVED
                        )
                );

        if (overlapping) {
            throw new LeaveOverlapException(employeeId);
        }
    }

    private void validateLeaveBalance(
            Long employeeId,
            Long leaveTypeId,
            int requestedDays) {

        int year = Year.now().getValue();

        var balance = leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(
                        employeeId,
                        leaveTypeId,
                        year
                )
                .orElseThrow(() ->
                        new InsufficientLeaveBalanceException(
                                employeeId,
                                leaveTypeId
                        )
                );

        if (balance.getRemainingDays() < requestedDays) {
            throw new InsufficientLeaveBalanceException(
                    employeeId,
                    leaveTypeId
            );
        }
    }
}