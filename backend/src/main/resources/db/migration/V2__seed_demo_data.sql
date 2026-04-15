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

INSERT INTO fab_layer (layer_code, layer_name, layer_type, sequence_no, description, status, created_by, updated_by)
SELECT 'LAYER-M1', 'Metal 1', 'METAL', 10, 'M1 互连层', 'ACTIVE', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM fab_layer WHERE layer_code = 'LAYER-M1');

INSERT INTO fab_layer (layer_code, layer_name, layer_type, sequence_no, description, status, created_by, updated_by)
SELECT 'LAYER-VIA1', 'Via 1', 'CONTACT', 20, 'VIA1 接触层', 'ACTIVE', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM fab_layer WHERE layer_code = 'LAYER-VIA1');

INSERT INTO fab_layer (layer_code, layer_name, layer_type, sequence_no, description, status, created_by, updated_by)
SELECT 'LAYER-M2', 'Metal 2', 'METAL', 30, 'M2 互连层', 'ACTIVE', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM fab_layer WHERE layer_code = 'LAYER-M2');

INSERT INTO fab_lot (lot_no, lot_status, source_type, priority_level, wafer_count, remark, dataset_id, created_by, updated_by)
SELECT 'LOT-DEMO-001', 'READY', 'DEMO', 'NORMAL', 3, '默认演示 lot', d.id, 0, 0
FROM demo_dataset d
WHERE d.dataset_code = 'NORMAL_BASELINE'
  AND NOT EXISTS (SELECT 1 FROM fab_lot WHERE lot_no = 'LOT-DEMO-001');

INSERT INTO fab_lot (lot_no, lot_status, source_type, priority_level, wafer_count, remark, dataset_id, created_by, updated_by)
SELECT 'LOT-DEMO-002', 'IN_ANALYSIS', 'DEMO', 'HIGH', 3, 'overlay 偏移场景 lot', d.id, 0, 0
FROM demo_dataset d
WHERE d.dataset_code = 'NORMAL_BASELINE'
  AND NOT EXISTS (SELECT 1 FROM fab_lot WHERE lot_no = 'LOT-DEMO-002');

INSERT INTO fab_wafer (lot_id, wafer_no, wafer_status, slot_no, diameter_mm, notch_direction, summary_tags_json, dataset_id, created_by, updated_by)
SELECT l.id, 'W01', 'READY', 1, 300.00, 'UP', JSON_OBJECT('scenario', 'BASELINE'), l.dataset_id, 0, 0
FROM fab_lot l
WHERE l.lot_no = 'LOT-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM fab_wafer w WHERE w.lot_id = l.id AND w.wafer_no = 'W01');

INSERT INTO fab_wafer (lot_id, wafer_no, wafer_status, slot_no, diameter_mm, notch_direction, summary_tags_json, dataset_id, created_by, updated_by)
SELECT l.id, 'W02', 'READY', 2, 300.00, 'UP', JSON_OBJECT('scenario', 'BASELINE'), l.dataset_id, 0, 0
FROM fab_lot l
WHERE l.lot_no = 'LOT-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM fab_wafer w WHERE w.lot_id = l.id AND w.wafer_no = 'W02');

INSERT INTO fab_wafer (lot_id, wafer_no, wafer_status, slot_no, diameter_mm, notch_direction, summary_tags_json, dataset_id, created_by, updated_by)
SELECT l.id, 'W03', 'ANALYZED', 3, 300.00, 'UP', JSON_OBJECT('scenario', 'BASELINE'), l.dataset_id, 0, 0
FROM fab_lot l
WHERE l.lot_no = 'LOT-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM fab_wafer w WHERE w.lot_id = l.id AND w.wafer_no = 'W03');

INSERT INTO recipe (recipe_code, recipe_name, recipe_type, status, description, layer_id, created_by, updated_by)
SELECT 'RC-BASE-001', 'Baseline Litho Recipe', 'BASE', 'ACTIVE', '演示基础配方', l.id, 0, 0
FROM fab_layer l
WHERE l.layer_code = 'LAYER-M1'
  AND NOT EXISTS (SELECT 1 FROM recipe WHERE recipe_code = 'RC-BASE-001');

INSERT INTO recipe_version (recipe_id, version_no, version_label, status, params_json, change_summary, created_by, updated_by)
SELECT r.id, 1, 'v1.0.0', 'RELEASED', JSON_OBJECT('dose', 42.5, 'focus', 0.12, 'alignmentStrategy', 'GLOBAL'), '初始演示版本', 0, 0
FROM recipe r
WHERE r.recipe_code = 'RC-BASE-001'
  AND NOT EXISTS (
      SELECT 1 FROM recipe_version rv WHERE rv.recipe_id = r.id AND rv.version_no = 1
  );
