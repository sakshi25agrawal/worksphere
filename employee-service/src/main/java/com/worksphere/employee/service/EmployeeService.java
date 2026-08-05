package com.worksphere.employee.service;
import com.worksphere.employee.dto.response.EmployeeDetailsResponse;
import com.worksphere.employee.dto.response.EmployeePageResponse;
import com.worksphere.employee.dto.request.EmployeeRequest;
import com.worksphere.employee.dto.response.EmployeeResponse;
import com.worksphere.employee.dto.response.EmployeeWithDepartmentResponse;


public interface EmployeeService {

  EmployeeResponse createEmployee(EmployeeRequest request);

//    EmployeeResponse  getEmployeeById(Long id);

  EmployeeWithDepartmentResponse getEmployeeWithDepartmentRest(Long id);

  EmployeeWithDepartmentResponse getEmployeeWithDepartmentFeign(Long id);

  EmployeePageResponse getAllEmployees(int page,
                                       int size,
                                       String sortBy,
                                       String sortDir);

  EmployeeResponse updateEmployee(Long id, EmployeeRequest request);

  void deleteEmployee(Long id);

  EmployeeDetailsResponse getEmployeeProfile(Long employeeId);


}
