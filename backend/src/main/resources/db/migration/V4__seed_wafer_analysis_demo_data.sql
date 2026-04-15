INSERT INTO simulation_task (
    task_no, task_name, lot_id, layer_id, recipe_version_id, scenario_type, status, priority_level,
    idempotency_key, result_summary_json, error_message, requested_by, created_by, updated_by
)
SELECT 'TASK-DEMO-004', 'Via overlay tuning run', lot.id, layer.id, rv.id, 'NORMAL', 'SUCCESS', 'HIGH',
       'TASK-DEMO-004', JSON_OBJECT('passRate', 0.903, 'avgOverlay', 6.41), NULL, u.id, 0, 0
FROM fab_lot lot
JOIN fab_layer layer ON layer.layer_code = 'LAYER-VIA1'
JOIN recipe_version rv ON rv.version_label = 'v1.1.0'
JOIN sys_user u ON u.username = 'demo'
WHERE lot.lot_no = 'LOT-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM simulation_task t WHERE t.task_no = 'TASK-DEMO-004');

INSERT INTO measurement_run (
    run_no, lot_id, wafer_id, layer_id, measurement_type, stage, source_type, tool_name,
    sampling_count, summary_json, status, created_by, updated_by
)
SELECT 'MR-DEMO-003', lot.id, wafer.id, layer.id, 'OVERLAY', 'PRE_ETCH', 'DEMO', 'KLA-3XX',
       9, JSON_OBJECT('meanOverlay', 6.41, 'p95Overlay', 10.80), 'COMPLETED', 0, 0
FROM fab_lot lot
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'W01'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-VIA1'
WHERE lot.lot_no = 'LOT-DEMO-001'
  AND NOT EXISTS (SELECT 1 FROM measurement_run mr WHERE mr.run_no = 'MR-DEMO-003');

INSERT INTO overlay_measurement_point (
    measurement_run_id, wafer_id, layer_id, target_code, x_coord, y_coord, overlay_x, overlay_y,
    overlay_magnitude, residual_value, focus_value, dose_value, confidence, is_outlier, created_by
)
SELECT mr.id, mr.wafer_id, mr.layer_id, p.target_code, p.x_coord, p.y_coord, p.overlay_x, p.overlay_y,
       p.overlay_magnitude, p.residual_value, p.focus_value, p.dose_value, p.confidence, p.is_outlier, 0
FROM measurement_run mr
JOIN (
    SELECT 'MR-DEMO-003' AS run_no, 'P01' AS target_code, -45.0 AS x_coord, -45.0 AS y_coord, 1.2 AS overlay_x, 1.1 AS overlay_y, 1.8 AS overlay_magnitude, 0.7 AS residual_value, 0.04 AS focus_value, 41.7 AS dose_value, 0.99 AS confidence, 0 AS is_outlier
    UNION ALL SELECT 'MR-DEMO-003','P02',0.0,-45.0,2.4,2.2,3.4,1.1,0.06,41.9,0.98,0
    UNION ALL SELECT 'MR-DEMO-003','P03',45.0,-45.0,3.5,3.2,4.8,1.6,0.08,42.1,0.97,0
    UNION ALL SELECT 'MR-DEMO-003','P04',-45.0,0.0,2.8,2.6,3.8,1.2,0.09,42.3,0.97,0
    UNION ALL SELECT 'MR-DEMO-003','P05',0.0,0.0,4.1,3.7,5.5,1.9,0.11,42.7,0.95,0
    UNION ALL SELECT 'MR-DEMO-003','P06',45.0,0.0,6.3,5.5,8.4,2.6,0.15,43.1,0.92,1
    UNION ALL SELECT 'MR-DEMO-003','P07',-45.0,45.0,3.4,3.0,4.5,1.5,0.10,42.5,0.96,0
    UNION ALL SELECT 'MR-DEMO-003','P08',0.0,45.0,5.2,4.6,7.0,2.1,0.13,42.9,0.94,0
    UNION ALL SELECT 'MR-DEMO-003','P09',45.0,45.0,8.5,7.8,11.6,3.4,0.18,43.6,0.89,1
) p ON p.run_no = mr.run_no
WHERE NOT EXISTS (
    SELECT 1 FROM overlay_measurement_point x
    WHERE x.measurement_run_id = mr.id AND x.target_code = p.target_code
);

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay,
    p95_overlay, pass_rate, pass_flag, warning_level, chart_snapshot_json, created_by
)
SELECT t.id, mr.wafer_id, mr.layer_id, mr.id, 6.41, 11.60, 1.80, 2.93, 10.80, 0.903, 1, 'MEDIUM',
       JSON_OBJECT('trend','responsive','sigma3',12.7), 0
FROM simulation_task t
JOIN measurement_run mr ON mr.run_no = 'MR-DEMO-003'
WHERE t.task_no = 'TASK-DEMO-004'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s WHERE s.task_id = t.id AND s.measurement_run_id = mr.id
  );
