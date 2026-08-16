package com.worksphere.payroll.kafka;

import com.worksphere.kafka.event.EmployeeCreatedEvent;
import com.worksphere.payroll.dto.request.CreatePayrollRequest;
import com.worksphere.payroll.repository.PayrollRepository;
import com.worksphere.payroll.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventConsumer {

    private final PayrollService payrollService;
    private final PayrollRepository payrollRepository;


    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 2000,
                    multiplier = 2.0
            )
    )
    @KafkaListener(
            topics = "employee-created",
            groupId = "worksphere-payroll-group"
    )
    public void handleEmployeeCreated(EmployeeCreatedEvent event) {

        log.info(
                "Received EmployeeCreatedEvent for employeeId={}",
                event.employeeId()
        );

        // Idempotency check
        if (payrollRepository.existsByEmployeeId(event.employeeId())) {

            log.info(
                    "Payroll already exists for employeeId={}. " +
                            "Skipping duplicate event.",
                    event.employeeId()
            );

            return;
        }
        CreatePayrollRequest request = new CreatePayrollRequest(
                event.employeeId(),
                BigDecimal.valueOf(event.salary()),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        payrollService.createPayroll(request);

        log.info(
                "Payroll created from EmployeeCreatedEvent for employeeId={}",
                event.employeeId()
        );
    }
    @DltHandler
    public void handleDlt(EmployeeCreatedEvent event) {

        log.error(
                "EmployeeCreatedEvent moved to DLT. employeeId={}",
                event.employeeId()
        );
    }
}