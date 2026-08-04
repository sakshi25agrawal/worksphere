package com.worksphere.payroll.controller;

import com.worksphere.payroll.dto.request.CreatePayrollRequest;
import com.worksphere.payroll.dto.request.UpdatePayrollRequest;
import com.worksphere.payroll.dto.response.PayrollResponse;
import com.worksphere.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Payroll Management",
        description = "APIs for managing employee payroll"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;


    @Operation(
            summary = "Create Payroll",
            description = "Creates payroll details for an employee."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Payroll created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Payroll already exists"
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PayrollResponse createPayroll(
            @Valid @RequestBody CreatePayrollRequest request) {

        return payrollService.createPayroll(request);
    }


    @Operation(
            summary = "Get Payroll by Employee ID",
            description = "Returns payroll details for the given employee."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payroll retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payroll not found"
            )
    })
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<PayrollResponse> getPayrollByEmployeeId(
            @PathVariable Long employeeId) {

        PayrollResponse payroll =
                payrollService.getPayrollByEmployeeId(employeeId);

        return ResponseEntity.ok(payroll);

    }
    @Operation(
            summary = "Get All Payrolls",
            description = "Returns all payroll records."
    )
    @GetMapping
    public List<PayrollResponse> getAllPayrolls() {

        return payrollService.getAllPayrolls();

    }

    @Operation(
            summary = "Update Payroll",
            description = "Updates payroll information and recalculates net salary."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Payroll updated successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payroll not found"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed"
            )
    })
    @PutMapping("/{payrollId}")
    public PayrollResponse updatePayroll(
            @PathVariable Long payrollId,
            @Valid @RequestBody UpdatePayrollRequest request) {

        return payrollService.updatePayroll(payrollId, request);
    }


    @Operation(
            summary = "Delete Payroll",
            description = "Deletes payroll details."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Payroll deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Payroll not found"
            )
    })
    @DeleteMapping("/{payrollId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayroll(
            @PathVariable Long payrollId) {

        payrollService.deletePayroll(payrollId);

    }

}