package com.worksphere.leave.client;

import com.worksphere.leave.dto.client.EmployeeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "employee-service",
        path = "/api/v1/employees"
)
public interface EmployeeFeignClient {

    @GetMapping("/{id}")
    EmployeeResponse getEmployeeById(
            @PathVariable("id") Long id
    );
}