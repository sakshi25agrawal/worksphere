USE auth_db;

CREATE TABLE IF NOT EXISTS app_users (
                                         id BIGINT NOT NULL AUTO_INCREMENT,
                                         username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_users_username UNIQUE (username)
    );

CREATE TABLE IF NOT EXISTS roles (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     name VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_name UNIQUE (name)
    );

CREATE TABLE IF NOT EXISTS permissions (
                                           id BIGINT NOT NULL AUTO_INCREMENT,
                                           code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (code)
    );

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT NOT NULL,
                                          role_id BIGINT NOT NULL,
                                          PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
    FOREIGN KEY (user_id) REFERENCES app_users(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
    FOREIGN KEY (role_id) REFERENCES roles(id)
    ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS role_permissions (
                                                id BIGINT NOT NULL AUTO_INCREMENT,
                                                role_id BIGINT NOT NULL,
                                                permission_id BIGINT NOT NULL,
                                                PRIMARY KEY (id),
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role
    FOREIGN KEY (role_id) REFERENCES roles(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
    ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS resources (
                                         id BIGINT NOT NULL AUTO_INCREMENT,
                                         code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    path_pattern VARCHAR(255) NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_resource_code UNIQUE (code),
    CONSTRAINT fk_resource_permission
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
    ON DELETE CASCADE
    );


-- ============================================================
-- ROLES
-- ============================================================

INSERT IGNORE INTO roles (name)
VALUES
    ('ADMIN'),
    ('MANAGER'),
    ('EMPLOYEE');


-- ============================================================
-- PERMISSIONS
-- ============================================================

INSERT IGNORE INTO permissions (code, name)
VALUES
    ('EMPLOYEE_VIEW', 'View employees'),
    ('EMPLOYEE_CREATE', 'Create employees'),
    ('EMPLOYEE_UPDATE', 'Update employees'),
    ('EMPLOYEE_DELETE', 'Delete employees'),

    ('DEPARTMENT_VIEW', 'View departments'),
    ('DEPARTMENT_CREATE', 'Create departments'),
    ('DEPARTMENT_UPDATE', 'Update departments'),
    ('DEPARTMENT_DELETE', 'Delete departments'),

    ('LEAVE_VIEW', 'View leave'),
    ('LEAVE_APPLY', 'Apply leave'),
    ('LEAVE_APPROVE', 'Approve leave'),
    ('LEAVE_REJECT', 'Reject leave'),

    ('PAYROLL_VIEW', 'View payroll'),
    ('PAYROLL_PROCESS', 'Process payroll');


-- ============================================================
-- ADMIN -> ALL PERMISSIONS
-- ============================================================

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'ADMIN';


-- ============================================================
-- MANAGER PERMISSIONS
-- ============================================================

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.code IN (
                 'EMPLOYEE_VIEW',
                 'EMPLOYEE_UPDATE',
                 'DEPARTMENT_VIEW',
                 'LEAVE_VIEW',
                 'LEAVE_APPROVE',
                 'LEAVE_REJECT',
                 'PAYROLL_VIEW'
    );


-- ============================================================
-- EMPLOYEE PERMISSIONS
-- ============================================================

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p
WHERE r.name = 'EMPLOYEE'
  AND p.code IN (
                 'EMPLOYEE_VIEW',
                 'LEAVE_VIEW',
                 'LEAVE_APPLY',
                 'PAYROLL_VIEW'
    );


-- ============================================================
-- RESOURCES
-- ============================================================

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'EMPLOYEE_CREATE',
    'Create employee',
    'POST',
    '/api/v1/employees',
    p.id
FROM permissions p
WHERE p.code = 'EMPLOYEE_CREATE';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'EMPLOYEE_VIEW',
    'View employees',
    'GET',
    '/api/v1/employees',
    p.id
FROM permissions p
WHERE p.code = 'EMPLOYEE_VIEW';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'EMPLOYEE_VIEW_BY_ID',
    'View employee by ID',
    'GET',
    '/api/v1/employees/{id}',
    p.id
FROM permissions p
WHERE p.code = 'EMPLOYEE_VIEW';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'EMPLOYEE_UPDATE',
    'Update employee',
    'PUT',
    '/api/v1/employees/{id}',
    p.id
FROM permissions p
WHERE p.code = 'EMPLOYEE_UPDATE';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'EMPLOYEE_DELETE',
    'Delete employee',
    'DELETE',
    '/api/v1/employees/{id}',
    p.id
FROM permissions p
WHERE p.code = 'EMPLOYEE_DELETE';

-- ============================================================
-- DEPARTMENT RESOURCES
-- ============================================================

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'DEPARTMENT_CREATE',
    'Create department',
    'POST',
    '/api/v1/departments',
    p.id
FROM permissions p
WHERE p.code = 'DEPARTMENT_CREATE';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'DEPARTMENT_VIEW',
    'View departments',
    'GET',
    '/api/v1/departments',
    p.id
FROM permissions p
WHERE p.code = 'DEPARTMENT_VIEW';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'DEPARTMENT_VIEW_BY_ID',
    'View department by ID',
    'GET',
    '/api/v1/departments/{id}',
    p.id
FROM permissions p
WHERE p.code = 'DEPARTMENT_VIEW';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'DEPARTMENT_UPDATE',
    'Update department',
    'PUT',
    '/api/v1/departments/{id}',
    p.id
FROM permissions p
WHERE p.code = 'DEPARTMENT_UPDATE';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'DEPARTMENT_DELETE',
    'Delete department',
    'DELETE',
    '/api/v1/departments/{id}',
    p.id
FROM permissions p
WHERE p.code = 'DEPARTMENT_DELETE';


-- ============================================================
-- LEAVE RESOURCES
-- ============================================================

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'LEAVE_APPLY',
    'Apply leave',
    'POST',
    '/api/v1/leaves',
    p.id
FROM permissions p
WHERE p.code = 'LEAVE_APPLY';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'LEAVE_VIEW_BY_ID',
    'View leave by ID',
    'GET',
    '/api/v1/leaves/{leaveId}',
    p.id
FROM permissions p
WHERE p.code = 'LEAVE_VIEW';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'LEAVE_VIEW_BY_EMPLOYEE',
    'View employee leaves',
    'GET',
    '/api/v1/leaves/employee/{employeeId}',
    p.id
FROM permissions p
WHERE p.code = 'LEAVE_VIEW';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'LEAVE_APPROVE',
    'Approve leave',
    'PUT',
    '/api/v1/leaves/{leaveId}/approve',
    p.id
FROM permissions p
WHERE p.code = 'LEAVE_APPROVE';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'LEAVE_REJECT',
    'Reject leave',
    'PUT',
    '/api/v1/leaves/{leaveId}/reject',
    p.id
FROM permissions p
WHERE p.code = 'LEAVE_REJECT';


-- ============================================================
-- PAYROLL RESOURCES
-- ============================================================

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'PAYROLL_CREATE',
    'Create payroll',
    'POST',
    '/api/payroll',
    p.id
FROM permissions p
WHERE p.code = 'PAYROLL_PROCESS';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'PAYROLL_VIEW',
    'View payroll',
    'GET',
    '/api/payroll',
    p.id
FROM permissions p
WHERE p.code = 'PAYROLL_VIEW';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'PAYROLL_VIEW_BY_EMPLOYEE',
    'View payroll by employee',
    'GET',
    '/api/payroll/employee/{employeeId}',
    p.id
FROM permissions p
WHERE p.code = 'PAYROLL_VIEW';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'PAYROLL_UPDATE',
    'Update payroll',
    'PUT',
    '/api/payroll/{payrollId}',
    p.id
FROM permissions p
WHERE p.code = 'PAYROLL_PROCESS';

INSERT IGNORE INTO resources
    (code, name, http_method, path_pattern, permission_id)
SELECT
    'PAYROLL_DELETE',
    'Delete payroll',
    'DELETE',
    '/api/payroll/{payrollId}',
    p.id
FROM permissions p
WHERE p.code = 'PAYROLL_PROCESS';