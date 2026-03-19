select * from roles;
select * from users;
select * from user_roles;
select * from refresh_tokens;

SHOW CREATE TABLE refresh_tokens;

SET SQL_SAFE_UPDATES = 0;

UPDATE roles
SET name = CONCAT('ROLE_', name)
WHERE name NOT LIKE 'ROLE_%';

SET SQL_SAFE_UPDATES = 1;

use ucenje;

DELETE FROM refresh_tokens;

DELETE FROM users WHERE email = 'nikola@test.com';
DELETE FROM user_roles;
DELETE FROM roles;

INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
INSERT INTO roles (name) VALUES ('ROLE_USER');

SELECT u.email, r.name
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON r.id = ur.role_id;
