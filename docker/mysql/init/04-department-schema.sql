USE worksphere_department;

CREATE TABLE IF NOT EXISTS departments (
                                           id BIGINT NOT NULL AUTO_INCREMENT,
                                           department_name VARCHAR(255) NOT NULL,
    department_code VARCHAR(255) NOT NULL,
    department_head VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_department_code UNIQUE (department_code)
    );
