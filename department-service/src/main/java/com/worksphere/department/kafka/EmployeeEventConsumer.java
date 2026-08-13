package com.worksphere.department.kafka;

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
            groupId = "worksphere-department-group"
    )
    public void handleEmployeeCreated(EmployeeCreatedEvent event) {

        log.info(
                "Received EmployeeCreatedEvent: employeeId={}, firstName={}, lastName={}, email={}, departmentId={}",
                event.employeeId(),
                event.firstName(),
                event.lastName(),
                event.email(),
                event.departmentId()
        );
    }
}