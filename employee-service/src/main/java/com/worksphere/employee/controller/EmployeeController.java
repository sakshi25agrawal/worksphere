package com.worksphere.employee.controller;

import com.worksphere.employee.dto.response.EmployeeDetailsResponse;
import com.worksphere.employee.dto.response.EmployeePageResponse;
import com.worksphere.employee.dto.request.EmployeeRequest;
import com.worksphere.employee.dto.response.EmployeeResponse;
import com.worksphere.employee.dto.response.EmployeeWithDepartmentResponse;
import com.worksphere.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employee API", description = "REST APIs for Employee Management")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create Employee",
            description = "Creates a new employee in the system." )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee Created Successfully"),
            @ApiResponse(responseCode = "400", description = "Validation Failed")
    })
    public EmployeeResponse createEmployee(
            @Valid @RequestBody EmployeeRequest request) {
        return employeeService.createEmployee(request);
    }

    @Operation(
            summary = "Get Employee By ID",
            description = "Returns employee details using employee ID."
    )
//    @GetMapping("/{id}")
//    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable("id") Long id) {
//
//        return ResponseEntity.ok(employeeService.getEmployeeById(id));
//    }

    // This API call will flow through RestClient implementation
    @GetMapping("/{id}/rest")
    public ResponseEntity<EmployeeWithDepartmentResponse> getEmployeeWithDepartmentRest(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(employeeService.getEmployeeWithDepartmentRest(id));
    }


    // This API call will flow through FeginClient implementation
    @GetMapping("/{id}/feign")
    public ResponseEntity<EmployeeWithDepartmentResponse> getEmployeeWithDepartmentFeign(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(employeeService.getEmployeeWithDepartmentFeign(id));
    }


    @Operation(
            summary = "Get All Employees",
            description = "Returns paginated employee list."
    )
    @GetMapping
    public ResponseEntity<EmployeePageResponse> getAllEmployees(

            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir

    ) {
        return ResponseEntity.ok(
                employeeService.getAllEmployees(
                        page,
                        size,
                        sortBy,
                        sortDir
                )
        );
    }

    @Operation(
            summary = "Update Employee",
            description = "Updates an existing employee."
    )
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable("id") Long id, @Valid
            @RequestBody EmployeeRequest request) {

        EmployeeResponse response = employeeService.updateEmployee(id, request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete Employee",
            description = "Deletes an employee by ID."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(
            @PathVariable("id") Long id) {

        employeeService.deleteEmployee(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get Employee Profile",
            description = "Returns employee, department and payroll details."
    )
    @GetMapping("/{employeeId}/profile")
    public ResponseEntity<EmployeeDetailsResponse> getEmployeeProfile(
            @PathVariable Long employeeId) {

        return ResponseEntity.ok(
                employeeService.getEmployeeProfile(employeeId)
        );
    }



}