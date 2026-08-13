package com.worksphere.employee.kafka;

import com.worksphere.kafka.event.EmployeeCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmployeeKafkaPublisher {

    private static final String EMPLOYEE_CREATED_TOPIC =
            "employee-created";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EmployeeKafkaPublisher(
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEmployeeCreated(EmployeeCreatedEvent event) {

        kafkaTemplate.send(
                EMPLOYEE_CREATED_TOPIC,
                String.valueOf(event.employeeId()),
                event
        );
    }
}