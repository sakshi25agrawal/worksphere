package com.worksphere.payroll.service;

import com.worksphere.common.exception.PayrollAlreadyExistsException;
import com.worksphere.common.exception.ResourceNotFoundException;
import com.worksphere.payroll.dto.request.CreatePayrollRequest;
import com.worksphere.payroll.dto.request.UpdatePayrollRequest;
import com.worksphere.payroll.dto.response.PayrollResponse;

import java.util.List;

public interface PayrollService {

    /**
     * Creates payroll details for an employee.
     *
     * Net salary is calculated using the following formula:
     *
     * Net Salary = Basic Salary + Bonus - Tax
     *
     * @param request payroll creation request
     * @return created payroll details
     * @throws PayrollAlreadyExistsException if payroll already exists
     */
    PayrollResponse createPayroll(CreatePayrollRequest request);

    /**
     * Retrieves payroll details for a given employee.
     *
     * @param employeeId employee identifier
     * @return payroll details
     * @throws ResourceNotFoundException if payroll does not exist
     */
    PayrollResponse getPayrollByEmployeeId(Long employeeId);

    /**
     * Retrieves all payroll records available in the system.
     *
     * @return list of payroll details
     */
    List<PayrollResponse> getAllPayrolls();

    /**
     * Updates payroll information.
     *
     * Employee ID is immutable and cannot be modified.
     * Net salary is recalculated after every successful update.
     *
     * @param payrollId payroll identifier
     * @param request updated payroll values
     * @return updated payroll details
     */
    PayrollResponse updatePayroll(
            Long payrollId,
            UpdatePayrollRequest request
    );

    /**
     * Deletes payroll information for the given payroll identifier.
     *
     * @param payrollId payroll identifier
     * @throws ResourceNotFoundException if payroll does not exist
     */
    void deletePayroll(Long payrollId);
}