package com.worksphere.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = {
                "com.worksphere.payroll",
                "com.worksphere.common"
        }
)
public class PayrollServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayrollServiceApplication.class, args);
    }
}