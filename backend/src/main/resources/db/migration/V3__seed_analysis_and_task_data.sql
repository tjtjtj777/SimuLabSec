INSERT INTO recipe_version (recipe_id, version_no, version_label, status, parent_version_id, params_json, change_summary, created_by, updated_by)
SELECT r.id, 2, 'v1.1.0', 'DRAFT', rv.id, JSON_OBJECT('dose', 43.1, 'focus', 0.09, 'alignmentStrategy', 'LOCAL', 'scanSpeed', 1.05), '增加局部对准策略并调整曝光参数', 0, 0
FROM recipe r
JOIN recipe_version rv ON rv.recipe_id = r.id AND rv.version_no = 1
WHERE r.recipe_code = 'RC-BASE-001'
  AND NOT EXISTS (
      SELECT 1 FROM recipe_version x WHERE x.recipe_id = r.id AND x.version_no = 2
  );

INSERT INTO simulation_task (
    task_no, task_name, lot_id, layer_id, recipe_version_id, scenario_type, status, priority_level,
    idempotency_key, result_summary_json, error_message, requested_by, created_by, updated_by
)
SELECT 'TASK-DEMO-001', 'Overlay baseline sweep', l.id, layer.id, rv.id, 'NORMAL', 'SUCCESS', 'NORMAL',
       'TASK-DEMO-001', JSON_OBJECT('passRate', 0.962, 'avgOverlay', 4.82), NULL, u.id, 0, 0
FROM fab_lot l
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M1'
JOIN recipe_version rv ON rv.version_label = 'v1.0.0'
JOIN sys_user u ON u.username = 'demo'
WHERE l.lot_no = 'LOT-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM simulation_task t WHERE t.task_no = 'TASK-DEMO-001');

INSERT INTO simulation_task (
    task_no, task_name, lot_id, layer_id, recipe_version_id, scenario_type, status, priority_level,
    idempotency_key, result_summary_json, error_message, requested_by, created_by, updated_by
)
SELECT 'TASK-DEMO-002', 'Overlay stress scenario', l.id, layer.id, rv.id, 'STRESS', 'RUNNING', 'HIGH',
       'TASK-DEMO-002', JSON_OBJECT('progress', 0.58), NULL, u.id, 0, 0
FROM fab_lot l
JOIN fab_layer layer ON layer.layer_code = 'LAYER-VIA1'
JOIN recipe_version rv ON rv.version_label = 'v1.1.0'
JOIN sys_user u ON u.username = 'demo'
WHERE l.lot_no = 'LOT-DEMO-002'
  AND NOT EXISTS (SELECT 1 FROM simulation_task t WHERE t.task_no = 'TASK-DEMO-002');

INSERT INTO simulation_task (
    task_no, task_name, lot_id, layer_id, recipe_version_id, scenario_type, status, priority_level,
    idempotency_key, result_summary_json, error_message, requested_by, created_by, updated_by
)
SELECT 'TASK-DEMO-003', 'Focus-dose correlation run', l.id, layer.id, rv.id, 'NORMAL', 'FAILED', 'NORMAL',
       'TASK-DEMO-003', JSON_OBJECT('passRate', 0.651, 'avgOverlay', 8.23), 'Dose drift exceeds control limit', u.id, 0, 0
FROM fab_lot l
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M2'
JOIN recipe_version rv ON rv.version_label = 'v1.0.0'
JOIN sys_user u ON u.username = 'demo'
WHERE l.lot_no = 'LOT-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM simulation_task t WHERE t.task_no = 'TASK-DEMO-003');

INSERT INTO measurement_run (
    run_no, lot_id, wafer_id, layer_id, measurement_type, stage, source_type, tool_name,
    sampling_count, summary_json, status, created_by, updated_by
)
SELECT 'MR-DEMO-001', lot.id, wafer.id, layer.id, 'OVERLAY', 'PRE_ETCH', 'DEMO', 'KLA-3XX',
       9, JSON_OBJECT('meanOverlay', 4.82, 'p95Overlay', 6.75), 'COMPLETED', 0, 0
FROM fab_lot lot
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'W01'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M1'
WHERE lot.lot_no = 'LOT-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM measurement_run mr WHERE mr.run_no = 'MR-DEMO-001');

INSERT INTO measurement_run (
    run_no, lot_id, wafer_id, layer_id, measurement_type, stage, source_type, tool_name,
    sampling_count, summary_json, status, created_by, updated_by
)
SELECT 'MR-DEMO-002', lot.id, wafer.id, layer.id, 'OVERLAY', 'POST_ETCH', 'DEMO', 'KLA-3XX',
       9, JSON_OBJECT('meanOverlay', 6.24, 'p95Overlay', 9.21), 'COMPLETED', 0, 0
FROM fab_lot lot
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'W02'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-VIA1'
WHERE lot.lot_no = 'LOT-DEMO-002'
  AND NOT EXISTS (SELECT 1 FROM measurement_run mr WHERE mr.run_no = 'MR-DEMO-002');

INSERT INTO overlay_measurement_point (
    measurement_run_id, wafer_id, layer_id, target_code, x_coord, y_coord, overlay_x, overlay_y,
    overlay_magnitude, residual_value, focus_value, dose_value, confidence, is_outlier, created_by
)
SELECT mr.id, mr.wafer_id, mr.layer_id, p.target_code, p.x_coord, p.y_coord, p.overlay_x, p.overlay_y,
       p.overlay_magnitude, p.residual_value, p.focus_value, p.dose_value, p.confidence, p.is_outlier, 0
FROM measurement_run mr
JOIN (
    SELECT 'MR-DEMO-001' AS run_no, 'P01' AS target_code, -45.0 AS x_coord, -45.0 AS y_coord, 2.1 AS overlay_x, 2.3 AS overlay_y, 3.1 AS overlay_magnitude, 1.1 AS residual_value, 0.08 AS focus_value, 42.1 AS dose_value, 0.98 AS confidence, 0 AS is_outlier
    UNION ALL SELECT 'MR-DEMO-001','P02',0.0,-45.0,2.9,2.6,3.9,1.3,0.09,42.3,0.97,0
    UNION ALL SELECT 'MR-DEMO-001','P03',45.0,-45.0,3.2,2.8,4.3,1.6,0.10,42.7,0.96,0
    UNION ALL SELECT 'MR-DEMO-001','P04',-45.0,0.0,2.4,2.2,3.3,1.2,0.07,42.0,0.99,0
    UNION ALL SELECT 'MR-DEMO-001','P05',0.0,0.0,2.8,2.7,3.8,1.4,0.08,42.2,0.98,0
    UNION ALL SELECT 'MR-DEMO-001','P06',45.0,0.0,3.6,3.1,4.8,1.8,0.11,42.8,0.95,0
    UNION ALL SELECT 'MR-DEMO-001','P07',-45.0,45.0,2.5,2.4,3.5,1.1,0.09,42.4,0.98,0
    UNION ALL SELECT 'MR-DEMO-001','P08',0.0,45.0,3.0,2.9,4.2,1.6,0.10,42.6,0.97,0
    UNION ALL SELECT 'MR-DEMO-001','P09',45.0,45.0,4.2,3.8,5.7,2.2,0.13,43.0,0.92,1
    UNION ALL SELECT 'MR-DEMO-002','P01',-45.0,-45.0,3.2,3.0,4.4,1.5,0.11,42.9,0.95,0
    UNION ALL SELECT 'MR-DEMO-002','P02',0.0,-45.0,4.1,3.6,5.5,2.1,0.13,43.2,0.93,0
    UNION ALL SELECT 'MR-DEMO-002','P03',45.0,-45.0,4.9,4.4,6.6,2.5,0.14,43.8,0.91,1
    UNION ALL SELECT 'MR-DEMO-002','P04',-45.0,0.0,3.5,3.1,4.7,1.8,0.10,42.7,0.96,0
    UNION ALL SELECT 'MR-DEMO-002','P05',0.0,0.0,4.0,3.7,5.4,2.0,0.12,43.1,0.94,0
    UNION ALL SELECT 'MR-DEMO-002','P06',45.0,0.0,5.4,4.9,7.3,2.8,0.15,44.0,0.89,1
    UNION ALL SELECT 'MR-DEMO-002','P07',-45.0,45.0,3.6,3.2,4.9,1.7,0.11,42.8,0.95,0
    UNION ALL SELECT 'MR-DEMO-002','P08',0.0,45.0,4.3,3.9,5.8,2.2,0.13,43.4,0.92,0
    UNION ALL SELECT 'MR-DEMO-002','P09',45.0,45.0,5.8,5.2,7.8,3.1,0.16,44.2,0.88,1
) p ON p.run_no = mr.run_no
WHERE NOT EXISTS (
    SELECT 1 FROM overlay_measurement_point x
    WHERE x.measurement_run_id = mr.id AND x.target_code = p.target_code
);

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay,
    p95_overlay, pass_rate, pass_flag, warning_level, chart_snapshot_json, created_by
)
SELECT t.id, mr.wafer_id, mr.layer_id, mr.id, 4.82, 6.75, 2.80, 1.09, 6.45, 0.962, 1, 'LOW',
       JSON_OBJECT('trend','stable','sigma3',7.2), 0
FROM simulation_task t
JOIN measurement_run mr ON mr.run_no = 'MR-DEMO-001'
WHERE t.task_no = 'TASK-DEMO-001'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s WHERE s.task_id = t.id AND s.measurement_run_id = mr.id
  );

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay,
    p95_overlay, pass_rate, pass_flag, warning_level, chart_snapshot_json, created_by
)
SELECT t.id, mr.wafer_id, mr.layer_id, mr.id, 6.24, 9.21, 3.90, 1.63, 8.98, 0.784, 0, 'MEDIUM',
       JSON_OBJECT('trend','drifting','sigma3',10.3), 0
FROM simulation_task t
JOIN measurement_run mr ON mr.run_no = 'MR-DEMO-002'
WHERE t.task_no = 'TASK-DEMO-003'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s WHERE s.task_id = t.id AND s.measurement_run_id = mr.id
  );

INSERT INTO sub_recipe (
    sub_recipe_code, recipe_version_id, source_task_id, lot_id, wafer_id, status, generation_type,
    param_delta_json, param_set_json, export_format, created_by, updated_by
)
SELECT 'SUB-RC-001', rv.id, t.id, lot.id, wafer.id, 'READY', 'AUTO',
       JSON_OBJECT('dose', -0.3, 'focus', 0.02), JSON_OBJECT('dose', 42.2, 'focus', 0.14), 'JSON', 0, 0
FROM recipe_version rv
JOIN simulation_task t ON t.task_no = 'TASK-DEMO-001'
JOIN fab_lot lot ON lot.lot_no = 'LOT-DEMO-001'
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'W01'
WHERE rv.version_label = 'v1.0.0'
  AND NOT EXISTS (SELECT 1 FROM sub_recipe sr WHERE sr.sub_recipe_code = 'SUB-RC-001');
