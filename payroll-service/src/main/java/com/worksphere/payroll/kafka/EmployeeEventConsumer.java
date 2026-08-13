package com.worksphere.payroll.kafka;

import com.worksphere.kafka.event.EmployeeCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EmployeeEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EmployeeEventConsumer.class);

    @KafkaListener(
            topics = "employee-created",
            groupId = "worksphere-payroll-group"
    )
    public void handleEmployeeCreated(EmployeeCreatedEvent event) {

        log.info(
                "Payroll Service received EmployeeCreatedEvent: employeeId={}, salary={}, departmentId={}",
                event.employeeId(),
                event.salary(),
                event.departmentId()
        );
    }
}