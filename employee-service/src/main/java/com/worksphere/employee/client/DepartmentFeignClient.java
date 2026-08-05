package com.worksphere.employee.client;

import com.worksphere.employee.dto.external.DepartmentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "department-service"
)
public interface DepartmentFeignClient {

    @GetMapping("/api/v1/departments/{id}")
    DepartmentResponse getDepartment(@PathVariable("id")  Long id);
}