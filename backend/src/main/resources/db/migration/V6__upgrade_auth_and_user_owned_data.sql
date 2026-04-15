ALTER TABLE fab_lot
    ADD COLUMN owner_user_id BIGINT NULL AFTER product_id,
    ADD COLUMN is_demo TINYINT NOT NULL DEFAULT 0 AFTER owner_user_id;

INSERT INTO sys_role (role_code, role_name, description, status, created_by, updated_by)
SELECT 'USER', '普通用户', '平台普通用户角色', 'ACTIVE', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'USER');

UPDATE sys_user
SET password_hash = '{bcrypt}$2b$10$2LPf38WfpS4bNY6ach91EuUcL3g6rmicVXbSbYITpILurIrNMFIW2',
    updated_by = 0
WHERE username = 'demo';

UPDATE fab_lot
SET owner_user_id = NULL,
    is_demo = 1,
    source_type = 'DEMO',
    updated_by = 0
WHERE created_by = 0 OR source_type = 'DEMO';
