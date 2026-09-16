USE worksphere_employee;

CREATE TABLE IF NOT EXISTS employees (
                                         id BIGINT NOT NULL AUTO_INCREMENT,
                                         first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    salary DOUBLE,
    department_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_email UNIQUE (email)
    );
