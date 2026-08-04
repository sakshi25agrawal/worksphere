package com.worksphere.payroll.service.impl;

import com.worksphere.common.exception.PayrollAlreadyExistsException;
import com.worksphere.common.exception.ResourceNotFoundException;
import com.worksphere.payroll.dto.request.CreatePayrollRequest;
import com.worksphere.payroll.dto.request.UpdatePayrollRequest;
import com.worksphere.payroll.dto.response.PayrollResponse;
import com.worksphere.payroll.entity.Payroll;
import com.worksphere.payroll.mapper.PayrollMapper;
import com.worksphere.payroll.repository.PayrollRepository;
import com.worksphere.payroll.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollMapper payrollMapper;

    @Override
    public PayrollResponse createPayroll(CreatePayrollRequest request) {


        log.info("Creating payroll for employeeId={}", request.employeeId());
        if (payrollRepository.existsByEmployeeId(request.employeeId())) {

            log.warn(
                    "Payroll already exists for employeeId={}",
                    request.employeeId()
            );

            throw new PayrollAlreadyExistsException(request.employeeId());
        }

        Payroll payroll = payrollMapper.toEntity(request);

        payroll.setNetSalary(calculateNetSalary(
                payroll.getBasicSalary(),
                payroll.getBonus(),
                payroll.getTax()
        ));

        Payroll savedPayroll = payrollRepository.save(payroll);

        log.info(
                "Payroll created successfully. payrollId={}, employeeId={}",
                savedPayroll.getId(),
                savedPayroll.getEmployeeId()
        );
        return payrollMapper.toResponse(savedPayroll);
    }
    /**
     * Calculates the employee's net salary.
     *
     * Formula:
     *
     * Net Salary = Basic Salary + Bonus - Tax
     *
     * @param basicSalary employee basic salary
     * @param bonus employee bonus
     * @param tax employee tax deduction
     * @return calculated net salary
     */
    private BigDecimal calculateNetSalary(
            BigDecimal basicSalary,
            BigDecimal bonus,
            BigDecimal tax) {

        return basicSalary
                .add(bonus)
                .subtract(tax);
    }

    @Override
    public PayrollResponse getPayrollByEmployeeId(Long employeeId) {

        log.info("Fetching payroll for employeeId={}", employeeId);
        Payroll payroll = payrollRepository
                .findByEmployeeId(employeeId)
                .orElseThrow(() -> {
                    log.warn("Payroll not found for employeeId={}", employeeId);
                    return new ResourceNotFoundException(
                            "Payroll",
                            "employeeId",
                            employeeId
                    );
                });


        return payrollMapper.toResponse(payroll);
    }


    @Override
    public List<PayrollResponse> getAllPayrolls() {
        log.info("Fetching all payroll records");

        List<PayrollResponse> payrolls = payrollRepository.findAll()
                .stream()
                .map(payrollMapper::toResponse)
                .toList();

        log.info("Found {} payroll records", payrolls.size());

        return payrolls;
    }

    @Override
    public PayrollResponse updatePayroll(
            Long payrollId,
            UpdatePayrollRequest request) {
        log.info("Updating payroll id={}", payrollId);
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payroll",
                                "id",
                                payrollId
                        ));

        payrollMapper.updateEntity(request, payroll);

        payroll.setNetSalary(
                calculateNetSalary(
                        payroll.getBasicSalary(),
                        payroll.getBonus(),
                        payroll.getTax()
                )
        );

        Payroll updatedPayroll = payrollRepository.save(payroll);
        log.info("Payroll updated successfully. payrollId={}", payrollId);
        return payrollMapper.toResponse(updatedPayroll);
    }

    @Override
    public void deletePayroll(Long payrollId) {
        log.info("Deleting payroll id={}", payrollId);
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payroll",
                                "id",
                                payrollId
                        ));

        payrollRepository.delete(payroll);
        log.info("Payroll deleted successfully. payrollId={}", payrollId);
    }
}