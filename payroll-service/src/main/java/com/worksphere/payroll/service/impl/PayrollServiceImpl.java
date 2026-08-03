package com.worksphere.payroll.service.impl;

import com.worksphere.common.exception.PayrollAlreadyExistsException;
import com.worksphere.common.exception.ResourceNotFoundException;
import com.worksphere.payroll.dto.request.CreatePayrollRequest;
import com.worksphere.payroll.dto.response.PayrollResponse;
import com.worksphere.payroll.entity.Payroll;
import com.worksphere.payroll.mapper.PayrollMapper;
import com.worksphere.payroll.repository.PayrollRepository;
import com.worksphere.payroll.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollMapper payrollMapper;

    @Override
    public PayrollResponse createPayroll(CreatePayrollRequest request) {

        if (payrollRepository.existsByEmployeeId(request.employeeId())) {
            throw new PayrollAlreadyExistsException(request.employeeId());
        }

        Payroll payroll = payrollMapper.toEntity(request);

        payroll.setNetSalary(calculateNetSalary(
                payroll.getBasicSalary(),
                payroll.getBonus(),
                payroll.getTax()
        ));

        Payroll savedPayroll = payrollRepository.save(payroll);

        return payrollMapper.toResponse(savedPayroll);
    }

    @Override
    public PayrollResponse getPayrollByEmployeeId(Long employeeId) {

        Payroll payroll = payrollRepository
                .findByEmployeeId(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payroll",
                                "employeeId",
                                employeeId
                        ));

        return payrollMapper.toResponse(payroll);
    }

    private BigDecimal calculateNetSalary(
            BigDecimal basicSalary,
            BigDecimal bonus,
            BigDecimal tax) {

        return basicSalary
                .add(bonus)
                .subtract(tax);
    }
}