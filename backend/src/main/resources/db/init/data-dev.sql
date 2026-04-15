INSERT INTO sys_role (role_code, role_name, description, status, created_by, updated_by)
SELECT 'ADMIN', '管理员', '系统管理员角色', 'ACTIVE', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'ADMIN');

INSERT INTO sys_user (user_code, username, password_hash, display_name, email, status, preferred_language, is_demo_account, created_by, updated_by)
SELECT 'U-DEMO-001', 'demo', '{noop}demo123456', 'Demo Engineer', 'demo@simulab.local', 'ACTIVE', 'en-US', 1, 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'demo');

INSERT INTO sys_user_role (user_id, role_id, created_by)
SELECT u.id, r.id, 0
FROM sys_user u
JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'demo'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id
  );

INSERT INTO demo_dataset (dataset_code, dataset_name, scenario_type, status, description, seed_version, created_by, updated_by)
SELECT 'NORMAL_BASELINE', 'Baseline Normal', 'NORMAL', 'ACTIVE', '默认演示场景', 'v1', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM demo_dataset WHERE dataset_code = 'NORMAL_BASELINE');

INSERT INTO recipe (recipe_code, recipe_name, recipe_type, status, description, created_by, updated_by)
SELECT 'RC-BASE-001', 'Baseline Litho Recipe', 'BASE', 'ACTIVE', '演示基础配方', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM recipe WHERE recipe_code = 'RC-BASE-001');

INSERT INTO recipe_version (recipe_id, version_no, version_label, status, params_json, change_summary, created_by, updated_by)
SELECT r.id, 1, 'v1.0.0', 'RELEASED', JSON_OBJECT('dose', 42.5, 'focus', 0.12, 'alignmentStrategy', 'GLOBAL'), '初始演示版本', 0, 0
FROM recipe r
WHERE r.recipe_code = 'RC-BASE-001'
  AND NOT EXISTS (
      SELECT 1 FROM recipe_version rv WHERE rv.recipe_id = r.id AND rv.version_no = 1
  );
