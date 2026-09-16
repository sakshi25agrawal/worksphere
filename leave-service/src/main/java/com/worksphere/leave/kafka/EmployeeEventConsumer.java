package com.worksphere.leave.kafka;

import com.worksphere.kafka.event.EmployeeCreatedEvent;
import com.worksphere.leave.service.LeaveBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventConsumer {

    private final LeaveBalanceService leaveBalanceService;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 2000,
                    multiplier = 2.0
            )
    )
    @KafkaListener(
            topics = "employee-created",
            groupId = "worksphere-leave-group"
    )
    public void handleEmployeeCreated(EmployeeCreatedEvent event) {

        log.info(
                "Received EmployeeCreatedEvent for employeeId={}",
                event.employeeId()
        );

        leaveBalanceService.initializeEmployeeBalances(
                event.employeeId()
        );

        log.info(
                "Leave balances initialized from EmployeeCreatedEvent " +
                        "for employeeId={}",
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