package com.worksphere.payroll.controller;

import com.worksphere.payroll.dto.PayrollResponse;
import com.worksphere.payroll.service.PayrollService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<PayrollResponse> getPayrollByEmployeeId(
            @PathVariable Long employeeId) {

        PayrollResponse payroll =
                payrollService.getPayrollByEmployeeId(employeeId);

        return ResponseEntity.ok(payroll);

    }

}