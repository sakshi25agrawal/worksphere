
package com.worksphere.employee.resilience;
import com.worksphere.employee.client.PayrollFeignClient;
import com.worksphere.employee.dto.external.PayrollResponse;
import org.springframework.stereotype.Service;

@Service
public class PayrollResilienceService {

    private final PayrollFeignClient payrollFeignClient;

    public PayrollResilienceService(PayrollFeignClient payrollFeignClient) {
        this.payrollFeignClient = payrollFeignClient;
    }

    public PayrollResponse getPayroll(Long employeeId) {

        return payrollFeignClient.getPayrollByEmployeeId(employeeId);

    }
}