package com.worksphere.payroll.controller;

import com.worksphere.payroll.dto.request.CreatePayrollRequest;
import com.worksphere.payroll.dto.response.PayrollResponse;
import com.worksphere.payroll.service.PayrollService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PayrollResponse createPayroll(
            @Valid @RequestBody CreatePayrollRequest request) {

        return payrollService.createPayroll(request);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<PayrollResponse> getPayrollByEmployeeId(
            @PathVariable Long employeeId) {

        PayrollResponse payroll =
                payrollService.getPayrollByEmployeeId(employeeId);

        return ResponseEntity.ok(payroll);

    }

    @GetMapping
    public List<PayrollResponse> getAllPayrolls() {

        return payrollService.getAllPayrolls();

    }

}