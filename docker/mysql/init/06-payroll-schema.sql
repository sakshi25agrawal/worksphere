USE worksphere_payroll;

CREATE TABLE IF NOT EXISTS payroll (
                                       id BIGINT NOT NULL AUTO_INCREMENT,
                                       employee_id BIGINT NOT NULL,
                                       basic_salary DECIMAL(15,2) NOT NULL,
    bonus DECIMAL(15,2) NOT NULL,
    tax DECIMAL(15,2) NOT NULL,
    net_salary DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payroll_employee_id UNIQUE (employee_id)
    );