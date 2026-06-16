-- Opaque test fixture: creates only the users needed to authenticate through the
-- real /api/auth/login endpoint. The tests remain black-box/opaque because the
-- test bodies do not query repositories or services; they exercise the app only
-- through HTTP requests and observable responses.

DELETE FROM purchases;
DELETE FROM users WHERE username IN (
    'opaque_admin',
    'opaque_employee',
    'opaque_client',
    'opaque_other_client',
    'opaque_logout_client'
);

INSERT INTO users (external_id, username, password, type, balance) VALUES
('opaque-admin-id', 'opaque_admin', '$2a$10$ElcM4Ergg1EN9BIVSjWlXOwjkoXPcdWP8KalvKMlf1jWdBJlLJ6Ae', 'ADMIN', 100.00),
('opaque-employee-id', 'opaque_employee', '$2a$10$ZHlnccLxu43BKYWu/bcLBepKG4rbXBkXe02zEfR.4SW9oaiXcfJgO', 'EMPLOYEE', NULL),
('opaque-client-id', 'opaque_client', '$2a$10$x8H1Lf0XSKArI5HcMeLfrukzNy.uC1kGxxPAtG5YnpdEvKv3PGNXy', 'CLIENT', 100.00),
('opaque-other-client-id', 'opaque_other_client', '$2a$10$nWfw6dwE2AcQx48KEqtSm.ex33myrVAERuQBtj6GQhoSjO.PwOxNu', 'CLIENT', 100.00),
('opaque-logout-client-id', 'opaque_logout_client', '$2a$10$x8H1Lf0XSKArI5HcMeLfrukzNy.uC1kGxxPAtG5YnpdEvKv3PGNXy', 'CLIENT', 100.00);
