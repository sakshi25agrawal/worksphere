package com.worksphere.employee.client;

import com.worksphere.common.exception.DepartmentServiceUnavailableException;
import com.worksphere.common.exception.RateLimitExceededException;
import com.worksphere.common.exception.ResourceNotFoundException;
import com.worksphere.employee.dto.DepartmentResponse;
import com.worksphere.employee.service.impl.EmployeeServiceImpl;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.stereotype.Service;



@Service
public class DepartmentClientService {

    private final DepartmentFeignClient departmentFeignClient;
    private static final Logger log =
            LoggerFactory.getLogger(EmployeeServiceImpl.class);
    public DepartmentClientService(DepartmentFeignClient departmentFeignClient) {
        this.departmentFeignClient = departmentFeignClient;
    }

    @Retry(
            name = "departmentService",
            fallbackMethod = "departmentFallback"
    )
    @CircuitBreaker(
            name = "departmentService",
            fallbackMethod = "departmentFallback"
    )
    @RateLimiter(
            name = "departmentServiceRateLimiter",
            fallbackMethod = "departmentFallback"
    )
    public DepartmentResponse getDepartment(Long departmentId) {

        try {

            return departmentFeignClient.getDepartment(departmentId);

        } catch (FeignException ex) {

            if (ex.status() == 404) {

                throw new ResourceNotFoundException(
                        "Department",
                        "id",
                        departmentId
                );
            }

            throw new DepartmentServiceUnavailableException(
                    "Department Service is temporarily unavailable. Please try again later."
            );
        }
    }

    private DepartmentResponse departmentFallback(
            Long departmentId,
            Exception ex) {

        log.error("Department fallback: {}", ex.getClass().getSimpleName());

        if (ex instanceof ResourceNotFoundException resourceNotFoundException) {
            throw resourceNotFoundException;
        }

        if (ex instanceof RateLimitExceededException rateLimitExceededException) {
            throw rateLimitExceededException;
        }

        if (ex instanceof DepartmentServiceUnavailableException departmentServiceUnavailableException) {
            throw departmentServiceUnavailableException;
        }

        if (ex instanceof RequestNotPermitted) {

            throw new RateLimitExceededException(
                    "Too many requests. Please try again after some time."
            );
        }

        if (ex instanceof CallNotPermittedException) {

            throw new DepartmentServiceUnavailableException(
                    "Department Service is temporarily unavailable. Please try again later."
            );
        }

        if (ex instanceof FeignException.NotFound) {

            throw new ResourceNotFoundException(
                    "Department",
                    "id",
                    departmentId
            );
        }

        if (ex instanceof FeignException) {

            throw new DepartmentServiceUnavailableException(
                    "Department Service is temporarily unavailable. Please try again later."
            );
        }

        throw new RuntimeException(ex);
    }
}