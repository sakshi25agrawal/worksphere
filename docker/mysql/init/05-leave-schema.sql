USE worksphere_leave;

CREATE TABLE IF NOT EXISTS leave_types (
                                           id BIGINT NOT NULL AUTO_INCREMENT,
                                           code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    annual_allocation INT NOT NULL,
    description VARCHAR(255),
    active BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_leave_type_code UNIQUE (code)
    );

CREATE TABLE IF NOT EXISTS leave_balances (
                                              id BIGINT NOT NULL AUTO_INCREMENT,
                                              employee_id BIGINT NOT NULL,
                                              leave_type_id BIGINT NOT NULL,
                                              allocated_days INT NOT NULL,
                                              used_days INT NOT NULL,
                                              remaining_days INT NOT NULL,
                                              year INT NOT NULL,
                                              PRIMARY KEY (id),
    CONSTRAINT fk_leave_balance_leave_type
    FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT uk_employee_leave_type_year
    UNIQUE (employee_id, leave_type_id, year)
    );

CREATE TABLE IF NOT EXISTS leave_requests (
                                              id BIGINT NOT NULL AUTO_INCREMENT,
                                              employee_id BIGINT NOT NULL,
                                              leave_type_id BIGINT NOT NULL,
                                              start_date DATE NOT NULL,
                                              end_date DATE NOT NULL,
                                              number_of_days INT NOT NULL,
                                              reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    applied_at DATETIME,
    approved_at DATETIME,
    rejected_at DATETIME,
    cancelled_at DATETIME,
    approver_id BIGINT,
    rejection_reason VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),

    CONSTRAINT fk_leave_request_leave_type
    FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),

    INDEX idx_leave_employee (employee_id),
    INDEX idx_leave_status (status),
    INDEX idx_leave_dates (start_date, end_date)
    );