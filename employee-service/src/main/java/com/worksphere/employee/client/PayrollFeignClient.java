package com.worksphere.employee.client;

import com.worksphere.employee.dto.external.PayrollResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "payroll-service")
public interface PayrollFeignClient {

    @GetMapping("/api/payroll/employee/{employeeId}")
    PayrollResponse getPayrollByEmployeeId(
            @PathVariable Long employeeId
    );
}