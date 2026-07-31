package com.worksphere.employee.service.impl;

import com.worksphere.common.exception.DuplicateResourceException;
import com.worksphere.common.exception.ResourceNotFoundException;
import com.worksphere.employee.client.DepartmentFeignClient;
import com.worksphere.employee.dto.*;
import com.worksphere.employee.entity.Employee;
import com.worksphere.employee.gateway.DepartmentGateway;
import com.worksphere.employee.mapper.EmployeeMapper;
import com.worksphere.employee.repository.EmployeeRepository;
import com.worksphere.employee.service.EmployeeService;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.worksphere.employee.client.DepartmentRestClient;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRestClient departmentRestClient;
    private final DepartmentFeignClient departmentFeignClient;
    private final DepartmentGateway departmentGateway;
    private static final Logger log =
            LoggerFactory.getLogger(EmployeeServiceImpl.class);
    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               DepartmentRestClient departmentClient,
                               DepartmentFeignClient departmentFeignClient, DepartmentGateway departmentGateway) {
        this.employeeRepository = employeeRepository;
        this.departmentRestClient = departmentClient;
        this.departmentFeignClient = departmentFeignClient;
        this.departmentGateway = departmentGateway;
    }

    @Override
    public EmployeeResponse createEmployee(EmployeeRequest request) {
        log.info("Creating employee with email: {}", request.email());

//        Employee employee = new Employee();
//
//        employee.setFirstName(request.firstName());
//        employee.setLastName(request.lastName());
//        employee.setEmail(request.email());
//        employee.setSalary(request.salary());
//        employee.setDepartmentId(request.departmentId());

        // Check duplicate email
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                    "Employee",
                    "email",
                    request.email()
            );
        }

        // Validate department exists
        log.info("Before calling department service");

        departmentGateway.getDepartment(request.departmentId());
        // Mapper Added
        Employee employee = EmployeeMapper.toEntity(request);

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created successfully with ID: {}", savedEmployee.getId());
//        return new EmployeeResponse(
//                savedEmployee.getId(),
//                savedEmployee.getFirstName(),
//                savedEmployee.getLastName(),
//                savedEmployee.getEmail(),
//                savedEmployee.getSalary(),
//                savedEmployee.getDepartmentId()
//        );
       //Added Mapper Feature
        return EmployeeMapper.toResponse(savedEmployee);
    }

//    @Override
//    public EmployeeResponse getEmployeeById(Long id) {
//
//        log.info("Fetching employee with ID: {}", id);
//
//        Employee employee = employeeRepository.findById(id)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Employee",
//                                "id",
//                                id
//                        ));
//        log.info("Employee found with ID: {}", id);
//
//        return new EmployeeResponse(
//                employee.getId(),
//                employee.getFirstName(),
//                employee.getLastName(),
//                employee.getEmail(),
//                employee.getSalary(),
//                employee.getDepartmentId()
//        );
//    }

    @Override
    public EmployeeWithDepartmentResponse getEmployeeWithDepartmentFeign(Long id) {

        log.info("Fetching employee with department using OpenFeign. Employee Id: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee",
                                "id",
                                id
                        ));

        DepartmentResponse department =
                departmentFeignClient.getDepartment(employee.getDepartmentId());

        return new EmployeeWithDepartmentResponse(
                EmployeeMapper.toResponse(employee),
                department
        );
    }

    @Override
    public EmployeeWithDepartmentResponse getEmployeeWithDepartmentRest(Long id) {

        log.info("Fetching employee with department using RestClient. Employee Id: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee",
                                "id",
                                id
                        ));

        DepartmentResponse department =
                departmentRestClient.getDepartment(employee.getDepartmentId());

        return new EmployeeWithDepartmentResponse(
                EmployeeMapper.toResponse(employee),
                department
        );
    }

    @Override
    public EmployeePageResponse getAllEmployees(
            int page,
            int size,
            String sortBy,
            String sortDir) {

        log.info("Fetching employees. Page={}, Size={}, SortBy={}, SortDir={}",
                page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Employee> employeePage = employeeRepository.findAll(pageable);

        List<EmployeeResponse> employees = employeePage.getContent()
                .stream()
//                .map(employee -> new EmployeeResponse(
//                        employee.getId(),
//                        employee.getFirstName(),
//                        employee.getLastName(),
//                        employee.getEmail(),
//                        employee.getSalary(),
//                        employee.getDepartmentId()
//                ))
                //Mapper added
                .map(EmployeeMapper::toResponse).toList();


        log.info("Successfully fetched {} employees",
                employeePage.getNumberOfElements());

        return new EmployeePageResponse(
                employees,
                employeePage.getNumber(),
                employeePage.getSize(),
                employeePage.getTotalElements(),
                employeePage.getTotalPages(),
                employeePage.isLast()
        );
    }


    @Override
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest request) {

        log.info("Updating employee with ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee",
                                "id",
                                id));

//        employee.setFirstName(request.firstName());
//        employee.setLastName(request.lastName());
//        employee.setEmail(request.email());
//        employee.setSalary(request.salary());
//        employee.setDepartmentId(request.departmentId());


        //Mapper Added
        EmployeeMapper.updateEntity(employee, request);

        Employee updatedEmployee = employeeRepository.save(employee);

        log.info("Employee updated successfully with ID: {}", id);

//        return new EmployeeResponse(
//                updatedEmployee.getId(),
//                updatedEmployee.getFirstName(),
//                updatedEmployee.getLastName(),
//                updatedEmployee.getEmail(),
//                updatedEmployee.getSalary(),
//                updatedEmployee.getDepartmentId()
//        );

        //Mapper added
        return EmployeeMapper.toResponse(updatedEmployee);
    }

    @Override
    public void deleteEmployee(Long id) {
        log.info("Deleting employee with ID: {}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee",
                                "id",
                                id));

        employeeRepository.delete(employee);

        log.info("Employee deleted successfully with ID: {}", id);
    }

}
