package com.worksphere.employee.client;

import com.worksphere.employee.dto.external.DepartmentResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;


@Component
public class DepartmentRestClient {

    private final RestClient restClient;


    public DepartmentRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public DepartmentResponse getDepartment(Long departmentId) {

        return restClient.get()
                .uri("/api/v1/departments/{id}", departmentId)
                .retrieve()
                .body(DepartmentResponse.class);
    }
}