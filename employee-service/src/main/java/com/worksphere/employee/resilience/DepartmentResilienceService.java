package com.worksphere.employee.resilience;

import com.worksphere.common.exception.DepartmentServiceUnavailableException;
import com.worksphere.common.exception.RateLimitExceededException;
import com.worksphere.common.exception.ResourceNotFoundException;
import com.worksphere.employee.client.DepartmentFeignClient;
import com.worksphere.employee.dto.DepartmentResponse;
import com.worksphere.employee.service.impl.EmployeeServiceImpl;
import feign.FeignException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

@Service
public class DepartmentResilienceService {

    private final DepartmentFeignClient departmentFeignClient;
    private static final Logger log =
            LoggerFactory.getLogger(EmployeeServiceImpl.class);

    public DepartmentResilienceService(DepartmentFeignClient departmentFeignClient) {
        this.departmentFeignClient = departmentFeignClient;
    }

    @Retry(
            name = "departmentServiceRetry",
            fallbackMethod = "departmentFallback")
    @CircuitBreaker(
            name = "departmentService",
            fallbackMethod = "departmentFallback")
    @RateLimiter(
            name = "departmentServiceRateLimiter",
            fallbackMethod = "departmentFallback")
    @Bulkhead(
            name = "departmentServiceBulkhead",
            type = Bulkhead.Type.SEMAPHORE,
            fallbackMethod = "departmentFallback")
    public DepartmentResponse getDepartment(Long departmentId) {

        log.error("Calling Department Service");

        return departmentFeignClient.getDepartment(departmentId);
    }

    private DepartmentResponse departmentFallback(
            Long departmentId,
            Exception ex) {

        log.error("Department fallback: {}", ex.getClass().getSimpleName());

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

        if (ex instanceof BulkheadFullException) {

            throw new DepartmentServiceUnavailableException(
                    "Department Service is busy. Please try again later."
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