USE auth_db;

-- =========================================================
-- 1. CLEAN EXISTING RBAC TABLES
-- =========================================================

DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS resources;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS roles;


-- =========================================================
-- 2. CREATE ROLES
-- =========================================================

CREATE TABLE roles (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       name VARCHAR(50) NOT NULL,

                       PRIMARY KEY (id),

                       CONSTRAINT uk_role_name
                           UNIQUE (name)
);


-- =========================================================
-- 3. CREATE PERMISSIONS
-- =========================================================

CREATE TABLE permissions (
                             id BIGINT NOT NULL AUTO_INCREMENT,
                             code VARCHAR(100) NOT NULL,
                             name VARCHAR(150) NOT NULL,

                             PRIMARY KEY (id),

                             CONSTRAINT uk_permission_code
                                 UNIQUE (code)
);


-- =========================================================
-- 4. CREATE USER_ROLES
-- =========================================================

CREATE TABLE user_roles (
                            id BIGINT NOT NULL AUTO_INCREMENT,
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,

                            PRIMARY KEY (id),

                            CONSTRAINT uk_user_role
                                UNIQUE (user_id, role_id),

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES app_users(id)
                                    ON DELETE CASCADE,

                            CONSTRAINT fk_user_roles_role
                                FOREIGN KEY (role_id)
                                    REFERENCES roles(id)
                                    ON DELETE CASCADE
);


-- =========================================================
-- 5. CREATE ROLE_PERMISSIONS
-- =========================================================

CREATE TABLE role_permissions (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  role_id BIGINT NOT NULL,
                                  permission_id BIGINT NOT NULL,

                                  PRIMARY KEY (id),

                                  CONSTRAINT uk_role_permission
                                      UNIQUE (role_id, permission_id),

                                  CONSTRAINT fk_role_permissions_role
                                      FOREIGN KEY (role_id)
                                          REFERENCES roles(id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT fk_role_permissions_permission
                                      FOREIGN KEY (permission_id)
                                          REFERENCES permissions(id)
                                          ON DELETE CASCADE
);


-- =========================================================
-- 6. CREATE RESOURCES
-- =========================================================

CREATE TABLE resources (
                           id BIGINT NOT NULL AUTO_INCREMENT,
                           code VARCHAR(100) NOT NULL,
                           name VARCHAR(150) NOT NULL,
                           http_method VARCHAR(10) NOT NULL,
                           path_pattern VARCHAR(255) NOT NULL,
                           permission_id BIGINT NOT NULL,

                           PRIMARY KEY (id),

                           CONSTRAINT uk_resource_code
                               UNIQUE (code),

                           CONSTRAINT fk_resource_permission
                               FOREIGN KEY (permission_id)
                                   REFERENCES permissions(id)
                                   ON DELETE CASCADE
);


-- =========================================================
-- 7. INSERT ROLES
-- =========================================================

INSERT INTO roles (name)
VALUES
    ('ADMIN'),
    ('MANAGER'),
    ('EMPLOYEE');


-- =========================================================
-- 8. INSERT PERMISSIONS
-- =========================================================

INSERT INTO permissions (code, name)
VALUES

    -- Employee
    ('EMPLOYEE_VIEW', 'View employees'),
    ('EMPLOYEE_CREATE', 'Create employees'),
    ('EMPLOYEE_UPDATE', 'Update employees'),
    ('EMPLOYEE_DELETE', 'Delete employees'),

    -- Department
    ('DEPARTMENT_VIEW', 'View departments'),
    ('DEPARTMENT_CREATE', 'Create departments'),
    ('DEPARTMENT_UPDATE', 'Update departments'),
    ('DEPARTMENT_DELETE', 'Delete departments'),

    -- Leave
    ('LEAVE_VIEW', 'View leave'),
    ('LEAVE_APPLY', 'Apply leave'),
    ('LEAVE_APPROVE', 'Approve leave'),
    ('LEAVE_REJECT', 'Reject leave'),

    -- Payroll
    ('PAYROLL_VIEW', 'View payroll'),
    ('PAYROLL_PROCESS', 'Process payroll');


-- =========================================================
-- 9. ADMIN → ALL PERMISSIONS
-- =========================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'ADMIN';


-- =========================================================
-- 10. MANAGER → SELECTED PERMISSIONS
-- =========================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
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


-- =========================================================
-- 11. EMPLOYEE → SELECTED PERMISSIONS
-- =========================================================

INSERT INTO role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM roles r
         JOIN permissions p
WHERE r.name = 'EMPLOYEE'
  AND p.code IN (
                 'EMPLOYEE_VIEW',

                 'LEAVE_VIEW',
                 'LEAVE_APPLY',

                 'PAYROLL_VIEW'
    );


-- =========================================================
-- 12. RESOURCE → PERMISSION MAPPING
-- =========================================================

INSERT INTO resources
(code, name, http_method, path_pattern, permission_id)

SELECT
    'EMPLOYEE_CREATE',
    'Create Employee',
    'POST',
    '/api/v1/employees',
    p.id
FROM permissions p
WHERE p.code = 'EMPLOYEE_CREATE';


INSERT INTO resources
(code, name, http_method, path_pattern, permission_id)

SELECT
    'EMPLOYEE_VIEW',
    'View Employees',
    'GET',
    '/api/v1/employees',
    p.id
FROM permissions p
WHERE p.code = 'EMPLOYEE_VIEW';


INSERT INTO resources
(code, name, http_method, path_pattern, permission_id)

SELECT
    'EMPLOYEE_VIEW_BY_ID',
    'View Employee By ID',
    'GET',
    '/api/v1/employees/{id}',
    p.id
FROM permissions p
WHERE p.code = 'EMPLOYEE_VIEW';


INSERT INTO resources
(code, name, http_method, path_pattern, permission_id)

SELECT
    'EMPLOYEE_UPDATE',
    'Update Employee',
    'PUT',
    '/api/v1/employees/{id}',
    p.id
FROM permissions p
WHERE p.code = 'EMPLOYEE_UPDATE';


INSERT INTO resources
(code, name, http_method, path_pattern, permission_id)

SELECT
    'EMPLOYEE_DELETE',
    'Delete Employee',
    'DELETE',
    '/api/v1/employees/{id}',
    p.id
FROM permissions p
WHERE p.code = 'EMPLOYEE_DELETE';


-- =========================================================
-- 13. ASSIGN ADMIN ROLE TO EXISTING ADMIN USER
-- =========================================================

INSERT INTO user_roles (user_id, role_id)
SELECT
    u.id,
    r.id
FROM app_users u
         JOIN roles r
WHERE u.username = 'admin'
  AND r.name = 'ADMIN';


-- =========================================================
-- 14. VERIFICATION
-- =========================================================

SELECT
    u.id AS user_id,
    u.username,
    r.name AS role_name
FROM app_users u
         JOIN user_roles ur
              ON u.id = ur.user_id
         JOIN roles r
              ON r.id = ur.role_id
WHERE u.username = 'admin';


SELECT
    r.name AS role_name,
    p.code AS permission_code
FROM roles r
         JOIN role_permissions rp
              ON r.id = rp.role_id
         JOIN permissions p
              ON p.id = rp.permission_id
WHERE r.name = 'ADMIN'
ORDER BY p.code;


SELECT
    code,
    http_method,
    path_pattern,
    permission_id
FROM resources
ORDER BY id;