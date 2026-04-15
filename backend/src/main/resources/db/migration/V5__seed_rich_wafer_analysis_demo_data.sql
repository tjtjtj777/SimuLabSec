INSERT INTO demo_dataset (dataset_code, dataset_name, scenario_type, status, description, seed_version, created_by, updated_by)
SELECT 'WAFER_STABLE_V2', 'Wafer Stable Dense Grid', 'NORMAL', 'ACTIVE', '稳定高密度点位演示场景', 'v5', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM demo_dataset WHERE dataset_code = 'WAFER_STABLE_V2');

INSERT INTO demo_dataset (dataset_code, dataset_name, scenario_type, status, description, seed_version, created_by, updated_by)
SELECT 'WAFER_OUTLIER_V2', 'Wafer Edge Outlier Dense Grid', 'ABNORMAL', 'ACTIVE', '边缘异常与局部热点演示场景', 'v5', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM demo_dataset WHERE dataset_code = 'WAFER_OUTLIER_V2');

INSERT INTO demo_dataset (dataset_code, dataset_name, scenario_type, status, description, seed_version, created_by, updated_by)
SELECT 'WAFER_DRIFT_V2', 'Wafer Parameter Drift Compare', 'DRIFT', 'ACTIVE', '参数漂移与配方优化前后对比场景', 'v5', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM demo_dataset WHERE dataset_code = 'WAFER_DRIFT_V2');

INSERT INTO fab_lot (lot_no, lot_status, source_type, priority_level, wafer_count, remark, dataset_id, created_by, updated_by)
SELECT 'LOT-STABLE-001', 'READY', 'DEMO', 'NORMAL', 2, '稳定场景 lot', d.id, 0, 0
FROM demo_dataset d
WHERE d.dataset_code = 'WAFER_STABLE_V2'
  AND NOT EXISTS (SELECT 1 FROM fab_lot WHERE lot_no = 'LOT-STABLE-001');

INSERT INTO fab_lot (lot_no, lot_status, source_type, priority_level, wafer_count, remark, dataset_id, created_by, updated_by)
SELECT 'LOT-OUTLIER-001', 'IN_ANALYSIS', 'DEMO', 'HIGH', 2, '局部异常场景 lot', d.id, 0, 0
FROM demo_dataset d
WHERE d.dataset_code = 'WAFER_OUTLIER_V2'
  AND NOT EXISTS (SELECT 1 FROM fab_lot WHERE lot_no = 'LOT-OUTLIER-001');

INSERT INTO fab_lot (lot_no, lot_status, source_type, priority_level, wafer_count, remark, dataset_id, created_by, updated_by)
SELECT 'LOT-DRIFT-001', 'IN_ANALYSIS', 'DEMO', 'HIGH', 2, '参数漂移前后对比 lot', d.id, 0, 0
FROM demo_dataset d
WHERE d.dataset_code = 'WAFER_DRIFT_V2'
  AND NOT EXISTS (SELECT 1 FROM fab_lot WHERE lot_no = 'LOT-DRIFT-001');

INSERT INTO fab_wafer (lot_id, wafer_no, wafer_status, slot_no, diameter_mm, notch_direction, summary_tags_json, dataset_id, created_by, updated_by)
SELECT l.id, 'WS01', 'READY', 1, 300.00, 'UP', JSON_OBJECT('scenario', 'STABLE'), l.dataset_id, 0, 0
FROM fab_lot l
WHERE l.lot_no = 'LOT-STABLE-001'
  AND NOT EXISTS (SELECT 1 FROM fab_wafer w WHERE w.lot_id = l.id AND w.wafer_no = 'WS01');

INSERT INTO fab_wafer (lot_id, wafer_no, wafer_status, slot_no, diameter_mm, notch_direction, summary_tags_json, dataset_id, created_by, updated_by)
SELECT l.id, 'WS02', 'READY', 2, 300.00, 'UP', JSON_OBJECT('scenario', 'STABLE'), l.dataset_id, 0, 0
FROM fab_lot l
WHERE l.lot_no = 'LOT-STABLE-001'
  AND NOT EXISTS (SELECT 1 FROM fab_wafer w WHERE w.lot_id = l.id AND w.wafer_no = 'WS02');

INSERT INTO fab_wafer (lot_id, wafer_no, wafer_status, slot_no, diameter_mm, notch_direction, summary_tags_json, dataset_id, created_by, updated_by)
SELECT l.id, 'WO01', 'READY', 1, 300.00, 'UP', JSON_OBJECT('scenario', 'OUTLIER'), l.dataset_id, 0, 0
FROM fab_lot l
WHERE l.lot_no = 'LOT-OUTLIER-001'
  AND NOT EXISTS (SELECT 1 FROM fab_wafer w WHERE w.lot_id = l.id AND w.wafer_no = 'WO01');

INSERT INTO fab_wafer (lot_id, wafer_no, wafer_status, slot_no, diameter_mm, notch_direction, summary_tags_json, dataset_id, created_by, updated_by)
SELECT l.id, 'WO02', 'READY', 2, 300.00, 'UP', JSON_OBJECT('scenario', 'OUTLIER'), l.dataset_id, 0, 0
FROM fab_lot l
WHERE l.lot_no = 'LOT-OUTLIER-001'
  AND NOT EXISTS (SELECT 1 FROM fab_wafer w WHERE w.lot_id = l.id AND w.wafer_no = 'WO02');

INSERT INTO fab_wafer (lot_id, wafer_no, wafer_status, slot_no, diameter_mm, notch_direction, summary_tags_json, dataset_id, created_by, updated_by)
SELECT l.id, 'WD01', 'READY', 1, 300.00, 'UP', JSON_OBJECT('scenario', 'DRIFT'), l.dataset_id, 0, 0
FROM fab_lot l
WHERE l.lot_no = 'LOT-DRIFT-001'
  AND NOT EXISTS (SELECT 1 FROM fab_wafer w WHERE w.lot_id = l.id AND w.wafer_no = 'WD01');

INSERT INTO fab_wafer (lot_id, wafer_no, wafer_status, slot_no, diameter_mm, notch_direction, summary_tags_json, dataset_id, created_by, updated_by)
SELECT l.id, 'WD02', 'READY', 2, 300.00, 'UP', JSON_OBJECT('scenario', 'DRIFT'), l.dataset_id, 0, 0
FROM fab_lot l
WHERE l.lot_no = 'LOT-DRIFT-001'
  AND NOT EXISTS (SELECT 1 FROM fab_wafer w WHERE w.lot_id = l.id AND w.wafer_no = 'WD02');

INSERT INTO simulation_task (
    task_no, task_name, lot_id, layer_id, recipe_version_id, scenario_type, status, priority_level,
    idempotency_key, result_summary_json, error_message, requested_by, created_by, updated_by
)
SELECT 'TASK-RICH-STABLE-001', 'Stable overlay dense map', lot.id, layer.id, rv.id, 'NORMAL', 'SUCCESS', 'NORMAL',
       'TASK-RICH-STABLE-001', JSON_OBJECT('passRate', 0.985, 'avgOverlay', 2.82), NULL, u.id, 0, 0
FROM fab_lot lot
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M1'
JOIN recipe_version rv ON rv.version_label = 'v1.0.0'
JOIN sys_user u ON u.username = 'demo'
WHERE lot.lot_no = 'LOT-STABLE-001'
  AND NOT EXISTS (SELECT 1 FROM simulation_task t WHERE t.task_no = 'TASK-RICH-STABLE-001');

INSERT INTO simulation_task (
    task_no, task_name, lot_id, layer_id, recipe_version_id, scenario_type, status, priority_level,
    idempotency_key, result_summary_json, error_message, requested_by, created_by, updated_by
)
SELECT 'TASK-RICH-OUTLIER-001', 'Edge hotspot inspection', lot.id, layer.id, rv.id, 'STRESS', 'SUCCESS', 'HIGH',
       'TASK-RICH-OUTLIER-001', JSON_OBJECT('passRate', 0.812, 'avgOverlay', 6.91), NULL, u.id, 0, 0
FROM fab_lot lot
JOIN fab_layer layer ON layer.layer_code = 'LAYER-VIA1'
JOIN recipe_version rv ON rv.version_label = 'v1.0.0'
JOIN sys_user u ON u.username = 'demo'
WHERE lot.lot_no = 'LOT-OUTLIER-001'
  AND NOT EXISTS (SELECT 1 FROM simulation_task t WHERE t.task_no = 'TASK-RICH-OUTLIER-001');

INSERT INTO simulation_task (
    task_no, task_name, lot_id, layer_id, recipe_version_id, scenario_type, status, priority_level,
    idempotency_key, result_summary_json, error_message, requested_by, created_by, updated_by
)
SELECT 'TASK-RICH-DRIFT-BEFORE', 'Dose drift before tuning', lot.id, layer.id, rv.id, 'DRIFT', 'SUCCESS', 'HIGH',
       'TASK-RICH-DRIFT-BEFORE', JSON_OBJECT('passRate', 0.741, 'avgOverlay', 7.84), NULL, u.id, 0, 0
FROM fab_lot lot
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M2'
JOIN recipe_version rv ON rv.version_label = 'v1.0.0'
JOIN sys_user u ON u.username = 'demo'
WHERE lot.lot_no = 'LOT-DRIFT-001'
  AND NOT EXISTS (SELECT 1 FROM simulation_task t WHERE t.task_no = 'TASK-RICH-DRIFT-BEFORE');

INSERT INTO simulation_task (
    task_no, task_name, lot_id, layer_id, recipe_version_id, scenario_type, status, priority_level,
    idempotency_key, result_summary_json, error_message, requested_by, created_by, updated_by
)
SELECT 'TASK-RICH-DRIFT-AFTER', 'Dose drift after tuning', lot.id, layer.id, rv.id, 'DRIFT', 'SUCCESS', 'HIGH',
       'TASK-RICH-DRIFT-AFTER', JSON_OBJECT('passRate', 0.931, 'avgOverlay', 4.11), NULL, u.id, 0, 0
FROM fab_lot lot
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M2'
JOIN recipe_version rv ON rv.version_label = 'v1.1.0'
JOIN sys_user u ON u.username = 'demo'
WHERE lot.lot_no = 'LOT-DRIFT-001'
  AND NOT EXISTS (SELECT 1 FROM simulation_task t WHERE t.task_no = 'TASK-RICH-DRIFT-AFTER');

INSERT INTO measurement_run (
    run_no, lot_id, wafer_id, layer_id, measurement_type, stage, source_type, tool_name, sampling_count,
    summary_json, status, created_by, updated_by
)
SELECT 'MR-RICH-STABLE-PRE', lot.id, wafer.id, layer.id, 'OVERLAY', 'PRE_ETCH', 'DEMO', 'KLA-3XX', 121,
       JSON_OBJECT('meanOverlay', 2.82, 'p95Overlay', 4.16), 'COMPLETED', 0, 0
FROM fab_lot lot
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'WS01'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M1'
WHERE lot.lot_no = 'LOT-STABLE-001'
  AND NOT EXISTS (SELECT 1 FROM measurement_run mr WHERE mr.run_no = 'MR-RICH-STABLE-PRE');

INSERT INTO measurement_run (
    run_no, lot_id, wafer_id, layer_id, measurement_type, stage, source_type, tool_name, sampling_count,
    summary_json, status, created_by, updated_by
)
SELECT 'MR-RICH-STABLE-POST', lot.id, wafer.id, layer.id, 'OVERLAY', 'POST_ETCH', 'DEMO', 'KLA-3XX', 121,
       JSON_OBJECT('meanOverlay', 2.34, 'p95Overlay', 3.65), 'COMPLETED', 0, 0
FROM fab_lot lot
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'WS01'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M1'
WHERE lot.lot_no = 'LOT-STABLE-001'
  AND NOT EXISTS (SELECT 1 FROM measurement_run mr WHERE mr.run_no = 'MR-RICH-STABLE-POST');

INSERT INTO measurement_run (
    run_no, lot_id, wafer_id, layer_id, measurement_type, stage, source_type, tool_name, sampling_count,
    summary_json, status, created_by, updated_by
)
SELECT 'MR-RICH-OUTLIER-PRE', lot.id, wafer.id, layer.id, 'OVERLAY', 'PRE_ETCH', 'DEMO', 'KLA-3XX', 121,
       JSON_OBJECT('meanOverlay', 6.91, 'p95Overlay', 11.47), 'COMPLETED', 0, 0
FROM fab_lot lot
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'WO01'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-VIA1'
WHERE lot.lot_no = 'LOT-OUTLIER-001'
  AND NOT EXISTS (SELECT 1 FROM measurement_run mr WHERE mr.run_no = 'MR-RICH-OUTLIER-PRE');

INSERT INTO measurement_run (
    run_no, lot_id, wafer_id, layer_id, measurement_type, stage, source_type, tool_name, sampling_count,
    summary_json, status, created_by, updated_by
)
SELECT 'MR-RICH-OUTLIER-POST', lot.id, wafer.id, layer.id, 'OVERLAY', 'POST_ETCH', 'DEMO', 'KLA-3XX', 121,
       JSON_OBJECT('meanOverlay', 5.54, 'p95Overlay', 9.72), 'COMPLETED', 0, 0
FROM fab_lot lot
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'WO01'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-VIA1'
WHERE lot.lot_no = 'LOT-OUTLIER-001'
  AND NOT EXISTS (SELECT 1 FROM measurement_run mr WHERE mr.run_no = 'MR-RICH-OUTLIER-POST');

INSERT INTO measurement_run (
    run_no, lot_id, wafer_id, layer_id, measurement_type, stage, source_type, tool_name, sampling_count,
    summary_json, status, created_by, updated_by
)
SELECT 'MR-RICH-DRIFT-BEFORE', lot.id, wafer.id, layer.id, 'OVERLAY', 'PRE_ETCH', 'DEMO', 'KLA-3XX', 121,
       JSON_OBJECT('meanOverlay', 7.84, 'p95Overlay', 12.21), 'COMPLETED', 0, 0
FROM fab_lot lot
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'WD01'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M2'
WHERE lot.lot_no = 'LOT-DRIFT-001'
  AND NOT EXISTS (SELECT 1 FROM measurement_run mr WHERE mr.run_no = 'MR-RICH-DRIFT-BEFORE');

INSERT INTO measurement_run (
    run_no, lot_id, wafer_id, layer_id, measurement_type, stage, source_type, tool_name, sampling_count,
    summary_json, status, created_by, updated_by
)
SELECT 'MR-RICH-DRIFT-AFTER', lot.id, wafer.id, layer.id, 'OVERLAY', 'POST_ETCH', 'DEMO', 'KLA-3XX', 121,
       JSON_OBJECT('meanOverlay', 4.11, 'p95Overlay', 6.98), 'COMPLETED', 0, 0
FROM fab_lot lot
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'WD01'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M2'
WHERE lot.lot_no = 'LOT-DRIFT-001'
  AND NOT EXISTS (SELECT 1 FROM measurement_run mr WHERE mr.run_no = 'MR-RICH-DRIFT-AFTER');

INSERT INTO overlay_measurement_point (
    measurement_run_id, wafer_id, layer_id, target_code, x_coord, y_coord, overlay_x, overlay_y,
    overlay_magnitude, residual_value, focus_value, dose_value, confidence, is_outlier, created_by
)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 120
)
SELECT
    mr.id,
    mr.wafer_id,
    mr.layer_id,
    CONCAT('P', LPAD(seq.n + 1, 3, '0')) AS target_code,
    ((seq.n % 11) - 5) * 10.0 AS x_coord,
    (FLOOR(seq.n / 11) - 5) * 10.0 AS y_coord,
    ROUND(0.65 + (((seq.n % 11) - 5) * 10.0) * 0.005 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.003, 6) AS overlay_x,
    ROUND(0.58 + (((seq.n % 11) - 5) * 10.0) * 0.004 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.002, 6) AS overlay_y,
    ROUND(SQRT(
        POW(0.65 + (((seq.n % 11) - 5) * 10.0) * 0.005 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.003, 2) +
        POW(0.58 + (((seq.n % 11) - 5) * 10.0) * 0.004 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.002, 2)
    ), 6) AS overlay_magnitude,
    ROUND(0.22 + ABS(((seq.n % 11) - 5) * 10.0) * 0.003 + ABS((FLOOR(seq.n / 11) - 5) * 10.0) * 0.002, 6) AS residual_value,
    ROUND(0.080 + (((seq.n % 11) - 5) * 10.0) * 0.0009 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.0003, 6) AS focus_value,
    ROUND(42.10 + (((seq.n % 11) - 5) * 10.0) * 0.008 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.006, 6) AS dose_value,
    ROUND(0.995 - (ABS(((seq.n % 11) - 5) * 10.0) + ABS((FLOOR(seq.n / 11) - 5) * 10.0)) * 0.0007, 4) AS confidence,
    CASE WHEN ABS((seq.n % 11) - 5) = 5 AND ABS(FLOOR(seq.n / 11) - 5) = 5 THEN 1 ELSE 0 END AS is_outlier,
    0
FROM measurement_run mr
JOIN seq
WHERE mr.run_no = 'MR-RICH-STABLE-PRE'
  AND NOT EXISTS (
      SELECT 1 FROM overlay_measurement_point x
      WHERE x.measurement_run_id = mr.id AND x.target_code = CONCAT('P', LPAD(seq.n + 1, 3, '0'))
  );

INSERT INTO overlay_measurement_point (
    measurement_run_id, wafer_id, layer_id, target_code, x_coord, y_coord, overlay_x, overlay_y,
    overlay_magnitude, residual_value, focus_value, dose_value, confidence, is_outlier, created_by
)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 120
)
SELECT
    mr.id,
    mr.wafer_id,
    mr.layer_id,
    CONCAT('P', LPAD(seq.n + 1, 3, '0')) AS target_code,
    ((seq.n % 11) - 5) * 10.0 AS x_coord,
    (FLOOR(seq.n / 11) - 5) * 10.0 AS y_coord,
    ROUND(0.52 + (((seq.n % 11) - 5) * 10.0) * 0.004 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.002, 6) AS overlay_x,
    ROUND(0.47 + (((seq.n % 11) - 5) * 10.0) * 0.003 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.0017, 6) AS overlay_y,
    ROUND(SQRT(
        POW(0.52 + (((seq.n % 11) - 5) * 10.0) * 0.004 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.002, 2) +
        POW(0.47 + (((seq.n % 11) - 5) * 10.0) * 0.003 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.0017, 2)
    ), 6) AS overlay_magnitude,
    ROUND(0.18 + ABS(((seq.n % 11) - 5) * 10.0) * 0.002 + ABS((FLOOR(seq.n / 11) - 5) * 10.0) * 0.0016, 6) AS residual_value,
    ROUND(0.078 + (((seq.n % 11) - 5) * 10.0) * 0.0008 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.0002, 6) AS focus_value,
    ROUND(42.00 + (((seq.n % 11) - 5) * 10.0) * 0.007 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.005, 6) AS dose_value,
    ROUND(0.997 - (ABS(((seq.n % 11) - 5) * 10.0) + ABS((FLOOR(seq.n / 11) - 5) * 10.0)) * 0.0006, 4) AS confidence,
    CASE WHEN ABS((seq.n % 11) - 5) = 5 AND ABS(FLOOR(seq.n / 11) - 5) = 5 THEN 1 ELSE 0 END AS is_outlier,
    0
FROM measurement_run mr
JOIN seq
WHERE mr.run_no = 'MR-RICH-STABLE-POST'
  AND NOT EXISTS (
      SELECT 1 FROM overlay_measurement_point x
      WHERE x.measurement_run_id = mr.id AND x.target_code = CONCAT('P', LPAD(seq.n + 1, 3, '0'))
  );

INSERT INTO overlay_measurement_point (
    measurement_run_id, wafer_id, layer_id, target_code, x_coord, y_coord, overlay_x, overlay_y,
    overlay_magnitude, residual_value, focus_value, dose_value, confidence, is_outlier, created_by
)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 120
)
SELECT
    mr.id,
    mr.wafer_id,
    mr.layer_id,
    CONCAT('P', LPAD(seq.n + 1, 3, '0')) AS target_code,
    ((seq.n % 11) - 5) * 10.0 AS x_coord,
    (FLOOR(seq.n / 11) - 5) * 10.0 AS y_coord,
    ROUND(
        1.30 + (((seq.n % 11) - 5) * 10.0) * 0.010 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.004
        + CASE
            WHEN POW(((seq.n % 11) - 5) * 10.0 - 20, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 10, 2) <= 400 THEN 5.80
            WHEN POW(((seq.n % 11) - 5) * 10.0 - 25, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 5, 2) <= 900 THEN 3.10
            ELSE 0
        END,
        6
    ) AS overlay_x,
    ROUND(
        1.10 + (((seq.n % 11) - 5) * 10.0) * 0.009 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.003
        + CASE
            WHEN POW(((seq.n % 11) - 5) * 10.0 - 20, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 10, 2) <= 400 THEN 4.90
            WHEN POW(((seq.n % 11) - 5) * 10.0 - 25, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 5, 2) <= 900 THEN 2.60
            ELSE 0
        END,
        6
    ) AS overlay_y,
    ROUND(SQRT(
        POW(
            1.30 + (((seq.n % 11) - 5) * 10.0) * 0.010 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.004
            + CASE
                WHEN POW(((seq.n % 11) - 5) * 10.0 - 20, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 10, 2) <= 400 THEN 5.80
                WHEN POW(((seq.n % 11) - 5) * 10.0 - 25, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 5, 2) <= 900 THEN 3.10
                ELSE 0
            END,
        2) +
        POW(
            1.10 + (((seq.n % 11) - 5) * 10.0) * 0.009 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.003
            + CASE
                WHEN POW(((seq.n % 11) - 5) * 10.0 - 20, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 10, 2) <= 400 THEN 4.90
                WHEN POW(((seq.n % 11) - 5) * 10.0 - 25, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 5, 2) <= 900 THEN 2.60
                ELSE 0
            END,
        2)
    ), 6) AS overlay_magnitude,
    ROUND(0.45 + ABS(((seq.n % 11) - 5) * 10.0) * 0.006 + ABS((FLOOR(seq.n / 11) - 5) * 10.0) * 0.005, 6) AS residual_value,
    ROUND(0.082 + (((seq.n % 11) - 5) * 10.0) * 0.0014 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.0007, 6) AS focus_value,
    ROUND(42.45 + (((seq.n % 11) - 5) * 10.0) * 0.013 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.010, 6) AS dose_value,
    ROUND(0.990 - (ABS(((seq.n % 11) - 5) * 10.0) + ABS((FLOOR(seq.n / 11) - 5) * 10.0)) * 0.0009, 4) AS confidence,
    CASE
        WHEN POW(((seq.n % 11) - 5) * 10.0 - 20, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 10, 2) <= 400 THEN 1
        WHEN POW(((seq.n % 11) - 5) * 10.0 - 25, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 5, 2) <= 900 THEN 1
        ELSE 0
    END AS is_outlier,
    0
FROM measurement_run mr
JOIN seq
WHERE mr.run_no = 'MR-RICH-OUTLIER-PRE'
  AND NOT EXISTS (
      SELECT 1 FROM overlay_measurement_point x
      WHERE x.measurement_run_id = mr.id AND x.target_code = CONCAT('P', LPAD(seq.n + 1, 3, '0'))
  );

INSERT INTO overlay_measurement_point (
    measurement_run_id, wafer_id, layer_id, target_code, x_coord, y_coord, overlay_x, overlay_y,
    overlay_magnitude, residual_value, focus_value, dose_value, confidence, is_outlier, created_by
)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 120
)
SELECT
    mr.id,
    mr.wafer_id,
    mr.layer_id,
    CONCAT('P', LPAD(seq.n + 1, 3, '0')) AS target_code,
    ((seq.n % 11) - 5) * 10.0 AS x_coord,
    (FLOOR(seq.n / 11) - 5) * 10.0 AS y_coord,
    ROUND(
        1.05 + (((seq.n % 11) - 5) * 10.0) * 0.009 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.003
        + CASE
            WHEN POW(((seq.n % 11) - 5) * 10.0 - 20, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 10, 2) <= 400 THEN 4.20
            WHEN POW(((seq.n % 11) - 5) * 10.0 - 25, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 5, 2) <= 900 THEN 2.10
            ELSE 0
        END,
        6
    ) AS overlay_x,
    ROUND(
        0.95 + (((seq.n % 11) - 5) * 10.0) * 0.008 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.002
        + CASE
            WHEN POW(((seq.n % 11) - 5) * 10.0 - 20, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 10, 2) <= 400 THEN 3.60
            WHEN POW(((seq.n % 11) - 5) * 10.0 - 25, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 5, 2) <= 900 THEN 1.70
            ELSE 0
        END,
        6
    ) AS overlay_y,
    ROUND(SQRT(
        POW(
            1.05 + (((seq.n % 11) - 5) * 10.0) * 0.009 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.003
            + CASE
                WHEN POW(((seq.n % 11) - 5) * 10.0 - 20, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 10, 2) <= 400 THEN 4.20
                WHEN POW(((seq.n % 11) - 5) * 10.0 - 25, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 5, 2) <= 900 THEN 2.10
                ELSE 0
            END,
        2) +
        POW(
            0.95 + (((seq.n % 11) - 5) * 10.0) * 0.008 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.002
            + CASE
                WHEN POW(((seq.n % 11) - 5) * 10.0 - 20, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 10, 2) <= 400 THEN 3.60
                WHEN POW(((seq.n % 11) - 5) * 10.0 - 25, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 5, 2) <= 900 THEN 1.70
                ELSE 0
            END,
        2)
    ), 6) AS overlay_magnitude,
    ROUND(0.38 + ABS(((seq.n % 11) - 5) * 10.0) * 0.005 + ABS((FLOOR(seq.n / 11) - 5) * 10.0) * 0.004, 6) AS residual_value,
    ROUND(0.081 + (((seq.n % 11) - 5) * 10.0) * 0.0011 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.0005, 6) AS focus_value,
    ROUND(42.35 + (((seq.n % 11) - 5) * 10.0) * 0.011 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.009, 6) AS dose_value,
    ROUND(0.992 - (ABS(((seq.n % 11) - 5) * 10.0) + ABS((FLOOR(seq.n / 11) - 5) * 10.0)) * 0.0008, 4) AS confidence,
    CASE
        WHEN POW(((seq.n % 11) - 5) * 10.0 - 20, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 10, 2) <= 400 THEN 1
        WHEN POW(((seq.n % 11) - 5) * 10.0 - 25, 2) + POW((FLOOR(seq.n / 11) - 5) * 10.0 + 5, 2) <= 900 THEN 1
        ELSE 0
    END AS is_outlier,
    0
FROM measurement_run mr
JOIN seq
WHERE mr.run_no = 'MR-RICH-OUTLIER-POST'
  AND NOT EXISTS (
      SELECT 1 FROM overlay_measurement_point x
      WHERE x.measurement_run_id = mr.id AND x.target_code = CONCAT('P', LPAD(seq.n + 1, 3, '0'))
  );

INSERT INTO overlay_measurement_point (
    measurement_run_id, wafer_id, layer_id, target_code, x_coord, y_coord, overlay_x, overlay_y,
    overlay_magnitude, residual_value, focus_value, dose_value, confidence, is_outlier, created_by
)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 120
)
SELECT
    mr.id,
    mr.wafer_id,
    mr.layer_id,
    CONCAT('P', LPAD(seq.n + 1, 3, '0')) AS target_code,
    ((seq.n % 11) - 5) * 10.0 AS x_coord,
    (FLOOR(seq.n / 11) - 5) * 10.0 AS y_coord,
    ROUND(2.25 + (((seq.n % 11) - 5) * 10.0) * 0.015 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.007, 6) AS overlay_x,
    ROUND(2.05 + (((seq.n % 11) - 5) * 10.0) * 0.013 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.006, 6) AS overlay_y,
    ROUND(SQRT(
        POW(2.25 + (((seq.n % 11) - 5) * 10.0) * 0.015 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.007, 2) +
        POW(2.05 + (((seq.n % 11) - 5) * 10.0) * 0.013 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.006, 2)
    ), 6) AS overlay_magnitude,
    ROUND(0.52 + ABS(((seq.n % 11) - 5) * 10.0) * 0.007 + ABS((FLOOR(seq.n / 11) - 5) * 10.0) * 0.006, 6) AS residual_value,
    ROUND(0.090 + (((seq.n % 11) - 5) * 10.0) * 0.0018 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.0010, 6) AS focus_value,
    ROUND(43.00 + (((seq.n % 11) - 5) * 10.0) * 0.017 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.013, 6) AS dose_value,
    ROUND(0.988 - (ABS(((seq.n % 11) - 5) * 10.0) + ABS((FLOOR(seq.n / 11) - 5) * 10.0)) * 0.0010, 4) AS confidence,
    CASE WHEN ABS((seq.n % 11) - 5) = 5 OR ABS(FLOOR(seq.n / 11) - 5) = 5 THEN 1 ELSE 0 END AS is_outlier,
    0
FROM measurement_run mr
JOIN seq
WHERE mr.run_no = 'MR-RICH-DRIFT-BEFORE'
  AND NOT EXISTS (
      SELECT 1 FROM overlay_measurement_point x
      WHERE x.measurement_run_id = mr.id AND x.target_code = CONCAT('P', LPAD(seq.n + 1, 3, '0'))
  );

INSERT INTO overlay_measurement_point (
    measurement_run_id, wafer_id, layer_id, target_code, x_coord, y_coord, overlay_x, overlay_y,
    overlay_magnitude, residual_value, focus_value, dose_value, confidence, is_outlier, created_by
)
WITH RECURSIVE seq AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 120
)
SELECT
    mr.id,
    mr.wafer_id,
    mr.layer_id,
    CONCAT('P', LPAD(seq.n + 1, 3, '0')) AS target_code,
    ((seq.n % 11) - 5) * 10.0 AS x_coord,
    (FLOOR(seq.n / 11) - 5) * 10.0 AS y_coord,
    ROUND(1.10 + (((seq.n % 11) - 5) * 10.0) * 0.008 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.004, 6) AS overlay_x,
    ROUND(1.00 + (((seq.n % 11) - 5) * 10.0) * 0.007 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.003, 6) AS overlay_y,
    ROUND(SQRT(
        POW(1.10 + (((seq.n % 11) - 5) * 10.0) * 0.008 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.004, 2) +
        POW(1.00 + (((seq.n % 11) - 5) * 10.0) * 0.007 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.003, 2)
    ), 6) AS overlay_magnitude,
    ROUND(0.34 + ABS(((seq.n % 11) - 5) * 10.0) * 0.004 + ABS((FLOOR(seq.n / 11) - 5) * 10.0) * 0.003, 6) AS residual_value,
    ROUND(0.084 + (((seq.n % 11) - 5) * 10.0) * 0.0011 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.0006, 6) AS focus_value,
    ROUND(42.55 + (((seq.n % 11) - 5) * 10.0) * 0.010 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.008, 6) AS dose_value,
    ROUND(0.992 - (ABS(((seq.n % 11) - 5) * 10.0) + ABS((FLOOR(seq.n / 11) - 5) * 10.0)) * 0.0008, 4) AS confidence,
    CASE WHEN ABS((seq.n % 11) - 5) = 5 OR ABS(FLOOR(seq.n / 11) - 5) = 5 THEN 1 ELSE 0 END AS is_outlier,
    0
FROM measurement_run mr
JOIN seq
WHERE mr.run_no = 'MR-RICH-DRIFT-AFTER'
  AND NOT EXISTS (
      SELECT 1 FROM overlay_measurement_point x
      WHERE x.measurement_run_id = mr.id AND x.target_code = CONCAT('P', LPAD(seq.n + 1, 3, '0'))
  );

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay, p95_overlay,
    pass_rate, pass_flag, warning_level, chart_snapshot_json, created_at, created_by
)
SELECT t.id, mr.wafer_id, mr.layer_id, mr.id, 2.82, 4.85, 0.94, 0.86, 4.16, 0.985, 1, 'LOW',
       JSON_OBJECT('scenario', 'stable', 'density', 121), '2026-04-01 10:00:00', 0
FROM simulation_task t
JOIN measurement_run mr ON mr.run_no = 'MR-RICH-STABLE-PRE'
WHERE t.task_no = 'TASK-RICH-STABLE-001'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s WHERE s.task_id = t.id AND s.measurement_run_id = mr.id
  );

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay, p95_overlay,
    pass_rate, pass_flag, warning_level, chart_snapshot_json, created_at, created_by
)
SELECT t.id, mr.wafer_id, mr.layer_id, mr.id, 2.34, 4.02, 0.78, 0.71, 3.65, 0.992, 1, 'LOW',
       JSON_OBJECT('scenario', 'stable', 'phase', 'post_etch'), '2026-04-02 10:00:00', 0
FROM simulation_task t
JOIN measurement_run mr ON mr.run_no = 'MR-RICH-STABLE-POST'
WHERE t.task_no = 'TASK-RICH-STABLE-001'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s WHERE s.task_id = t.id AND s.measurement_run_id = mr.id
  );

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay, p95_overlay,
    pass_rate, pass_flag, warning_level, chart_snapshot_json, created_at, created_by
)
SELECT t.id, mr.wafer_id, mr.layer_id, mr.id, 6.91, 13.62, 1.65, 3.12, 11.47, 0.812, 0, 'HIGH',
       JSON_OBJECT('scenario', 'outlier', 'hotspot', 'edge_cluster'), '2026-04-03 10:00:00', 0
FROM simulation_task t
JOIN measurement_run mr ON mr.run_no = 'MR-RICH-OUTLIER-PRE'
WHERE t.task_no = 'TASK-RICH-OUTLIER-001'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s WHERE s.task_id = t.id AND s.measurement_run_id = mr.id
  );

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay, p95_overlay,
    pass_rate, pass_flag, warning_level, chart_snapshot_json, created_at, created_by
)
SELECT t.id, mr.wafer_id, mr.layer_id, mr.id, 5.54, 10.48, 1.32, 2.41, 9.72, 0.875, 0, 'MEDIUM',
       JSON_OBJECT('scenario', 'outlier', 'phase', 'post_etch'), '2026-04-04 10:00:00', 0
FROM simulation_task t
JOIN measurement_run mr ON mr.run_no = 'MR-RICH-OUTLIER-POST'
WHERE t.task_no = 'TASK-RICH-OUTLIER-001'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s WHERE s.task_id = t.id AND s.measurement_run_id = mr.id
  );

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay, p95_overlay,
    pass_rate, pass_flag, warning_level, chart_snapshot_json, created_at, created_by
)
SELECT t.id, mr.wafer_id, mr.layer_id, mr.id, 7.84, 12.96, 2.21, 3.26, 12.21, 0.741, 0, 'HIGH',
       JSON_OBJECT('scenario', 'drift_before', 'recipe', 'v1.0.0'), '2026-04-05 10:00:00', 0
FROM simulation_task t
JOIN measurement_run mr ON mr.run_no = 'MR-RICH-DRIFT-BEFORE'
WHERE t.task_no = 'TASK-RICH-DRIFT-BEFORE'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s WHERE s.task_id = t.id AND s.measurement_run_id = mr.id
  );

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay, p95_overlay,
    pass_rate, pass_flag, warning_level, chart_snapshot_json, created_at, created_by
)
SELECT t.id, mr.wafer_id, mr.layer_id, mr.id, 4.11, 7.23, 1.12, 1.58, 6.98, 0.931, 1, 'LOW',
       JSON_OBJECT('scenario', 'drift_after', 'recipe', 'v1.1.0'), '2026-04-06 10:00:00', 0
FROM simulation_task t
JOIN measurement_run mr ON mr.run_no = 'MR-RICH-DRIFT-AFTER'
WHERE t.task_no = 'TASK-RICH-DRIFT-AFTER'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s WHERE s.task_id = t.id AND s.measurement_run_id = mr.id
  );

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay, p95_overlay,
    pass_rate, pass_flag, warning_level, chart_snapshot_json, created_at, created_by
)
WITH RECURSIVE day_series AS (
    SELECT 0 AS n
    UNION ALL
    SELECT n + 1 FROM day_series WHERE n < 8
)
SELECT
    t.id,
    wafer.id,
    layer.id,
    NULL,
    ROUND(7.6 - day_series.n * 0.44, 6) AS mean_overlay,
    ROUND(12.2 - day_series.n * 0.53, 6) AS max_overlay,
    ROUND(2.2 - day_series.n * 0.05, 6) AS min_overlay,
    ROUND(3.1 - day_series.n * 0.20, 6) AS std_overlay,
    ROUND(11.4 - day_series.n * 0.47, 6) AS p95_overlay,
    ROUND(0.72 + day_series.n * 0.025, 4) AS pass_rate,
    CASE WHEN day_series.n >= 5 THEN 1 ELSE 0 END AS pass_flag,
    CASE WHEN day_series.n >= 5 THEN 'LOW' ELSE 'MEDIUM' END AS warning_level,
    JSON_OBJECT('scenario', 'drift_timeline', 'point', day_series.n) AS chart_snapshot_json,
    DATE_ADD('2026-03-25 09:00:00', INTERVAL day_series.n DAY) AS created_at,
    0
FROM simulation_task t
JOIN fab_lot lot ON lot.id = t.lot_id AND lot.lot_no = 'LOT-DRIFT-001'
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'WD01'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M2'
CROSS JOIN day_series
WHERE t.task_no = 'TASK-RICH-DRIFT-AFTER'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s
      WHERE s.task_id = t.id AND s.wafer_id = wafer.id AND s.layer_id = layer.id
        AND DATE(s.created_at) = DATE(DATE_ADD('2026-03-25 09:00:00', INTERVAL day_series.n DAY))
  );
