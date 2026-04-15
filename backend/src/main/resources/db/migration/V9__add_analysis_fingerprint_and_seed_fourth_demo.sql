ALTER TABLE measurement_run
    ADD COLUMN analysis_fingerprint VARCHAR(64) NULL AFTER import_file_id;

CREATE INDEX idx_measurement_run_fingerprint
    ON measurement_run (analysis_fingerprint, status, deleted, id);

UPDATE measurement_run mr
JOIN fab_layer l ON l.id = mr.layer_id
SET mr.analysis_fingerprint = SHA2(
    CONCAT(
        'fingerprintVersion=v1|',
        'layerCode=', UPPER(l.layer_code), '|',
        'measurementType=OVERLAY|',
        'stage=PRE_ETCH|',
        'scannerCorrectionGain=1.000000|',
        'edgeGradient=1.200000|',
        'overlayBaseNm=3.200000|',
        'localHotspotStrength=0.800000|',
        'noiseLevel=0.120000|',
        'gridStep=0.500000|',
        'outlierThreshold=8.000000'
    ), 256
)
WHERE mr.run_no = 'MR-RICH-STABLE-PRE';

UPDATE measurement_run mr
JOIN fab_layer l ON l.id = mr.layer_id
SET mr.analysis_fingerprint = SHA2(
    CONCAT(
        'fingerprintVersion=v1|',
        'layerCode=', UPPER(l.layer_code), '|',
        'measurementType=OVERLAY|',
        'stage=PRE_ETCH|',
        'scannerCorrectionGain=1.150000|',
        'edgeGradient=1.900000|',
        'overlayBaseNm=3.600000|',
        'localHotspotStrength=1.600000|',
        'noiseLevel=0.180000|',
        'gridStep=0.500000|',
        'outlierThreshold=8.500000'
    ), 256
)
WHERE mr.run_no = 'MR-RICH-OUTLIER-PRE';

UPDATE measurement_run mr
JOIN fab_layer l ON l.id = mr.layer_id
SET mr.analysis_fingerprint = SHA2(
    CONCAT(
        'fingerprintVersion=v1|',
        'layerCode=', UPPER(l.layer_code), '|',
        'measurementType=OVERLAY|',
        'stage=POST_ETCH|',
        'scannerCorrectionGain=0.900000|',
        'edgeGradient=1.700000|',
        'overlayBaseNm=2.900000|',
        'localHotspotStrength=1.100000|',
        'noiseLevel=0.220000|',
        'gridStep=0.500000|',
        'outlierThreshold=7.800000'
    ), 256
)
WHERE mr.run_no = 'MR-RICH-DRIFT-AFTER';

INSERT INTO demo_dataset (dataset_code, dataset_name, scenario_type, status, description, seed_version, created_by, updated_by)
SELECT 'WAFER_NOISE_V2', 'Wafer Noise Stress Mix', 'NOISE', 'ACTIVE', '高噪声和局部偏移混合演示场景', 'v9', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM demo_dataset WHERE dataset_code = 'WAFER_NOISE_V2');

INSERT INTO fab_lot (lot_no, lot_status, source_type, priority_level, wafer_count, remark, dataset_id, owner_user_id, is_demo, created_by, updated_by)
SELECT 'LOT-NOISE-001', 'IN_ANALYSIS', 'DEMO', 'HIGH', 1, '高噪声演示 lot', d.id, NULL, 1, 0, 0
FROM demo_dataset d
WHERE d.dataset_code = 'WAFER_NOISE_V2'
  AND NOT EXISTS (SELECT 1 FROM fab_lot WHERE lot_no = 'LOT-NOISE-001');

INSERT INTO fab_wafer (lot_id, wafer_no, wafer_status, slot_no, diameter_mm, notch_direction, summary_tags_json, dataset_id, created_by, updated_by)
SELECT l.id, 'WN01', 'READY', 1, 300.00, 'UP', JSON_OBJECT('scenario', 'NOISE'), l.dataset_id, 0, 0
FROM fab_lot l
WHERE l.lot_no = 'LOT-NOISE-001'
  AND NOT EXISTS (SELECT 1 FROM fab_wafer w WHERE w.lot_id = l.id AND w.wafer_no = 'WN01');

INSERT INTO wafer_analysis_config (
    config_no, config_name, description, lot_no, wafer_no, layer_id, measurement_type, stage,
    scanner_correction_gain, overlay_base_nm, edge_gradient, local_hotspot_strength, noise_level,
    grid_step, outlier_threshold, status, created_by, updated_by
)
SELECT 'CFG-DEMO-NOISE', 'Demo Noise Stress', 'Noise-stress demo config for wafer analysis', 'LOT-NOISE-001', 'WN01', l.id,
       'OVERLAY', 'PRE_ETCH', 1.0600, 3.4500, 1.5500, 1.2500, 0.3200, 0.5000, 8.3000, 'ACTIVE', 0, 0
FROM fab_layer l
WHERE l.layer_code = 'LAYER-M1'
  AND NOT EXISTS (SELECT 1 FROM wafer_analysis_config WHERE config_no = 'CFG-DEMO-NOISE');

INSERT INTO simulation_task (
    task_no, task_name, lot_id, layer_id, recipe_version_id, scenario_type, status, priority_level,
    idempotency_key, result_summary_json, error_message, requested_by, created_by, updated_by
)
SELECT 'TASK-RICH-NOISE-001', 'Noise stress wafer baseline', lot.id, layer.id, rv.id, 'NOISE', 'SUCCESS', 'HIGH',
       'TASK-RICH-NOISE-001', JSON_OBJECT('passRate', 0.846, 'avgOverlay', 5.38), NULL, u.id, 0, 0
FROM fab_lot lot
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M1'
JOIN recipe_version rv ON rv.version_label = 'v1.0.0'
JOIN sys_user u ON u.username = 'demo'
WHERE lot.lot_no = 'LOT-NOISE-001'
  AND NOT EXISTS (SELECT 1 FROM simulation_task t WHERE t.task_no = 'TASK-RICH-NOISE-001');

INSERT INTO measurement_run (
    run_no, lot_id, wafer_id, layer_id, measurement_type, stage, source_type, tool_name, sampling_count,
    analysis_fingerprint, summary_json, status, created_by, updated_by
)
SELECT 'MR-RICH-NOISE-PRE', lot.id, wafer.id, layer.id, 'OVERLAY', 'PRE_ETCH', 'DEMO', 'KLA-3XX', 121,
       SHA2(
           CONCAT(
               'fingerprintVersion=v1|',
               'layerCode=', UPPER(layer.layer_code), '|',
               'measurementType=OVERLAY|',
               'stage=PRE_ETCH|',
               'scannerCorrectionGain=1.060000|',
               'edgeGradient=1.550000|',
               'overlayBaseNm=3.450000|',
               'localHotspotStrength=1.250000|',
               'noiseLevel=0.320000|',
               'gridStep=0.500000|',
               'outlierThreshold=8.300000'
           ), 256
       ),
       JSON_OBJECT('meanOverlay', 5.38, 'p95Overlay', 8.67), 'COMPLETED', 0, 0
FROM fab_lot lot
JOIN fab_wafer wafer ON wafer.lot_id = lot.id AND wafer.wafer_no = 'WN01'
JOIN fab_layer layer ON layer.layer_code = 'LAYER-M1'
WHERE lot.lot_no = 'LOT-NOISE-001'
  AND NOT EXISTS (SELECT 1 FROM measurement_run mr WHERE mr.run_no = 'MR-RICH-NOISE-PRE');

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
    ROUND(1.45 + (((seq.n % 11) - 5) * 10.0) * 0.011 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.006 + SIN(seq.n * 0.4) * 0.75, 6) AS overlay_x,
    ROUND(1.20 + (((seq.n % 11) - 5) * 10.0) * 0.009 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.004 + COS(seq.n * 0.5) * 0.62, 6) AS overlay_y,
    ROUND(SQRT(
        POW(1.45 + (((seq.n % 11) - 5) * 10.0) * 0.011 + ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.006 + SIN(seq.n * 0.4) * 0.75, 2) +
        POW(1.20 + (((seq.n % 11) - 5) * 10.0) * 0.009 - ((FLOOR(seq.n / 11) - 5) * 10.0) * 0.004 + COS(seq.n * 0.5) * 0.62, 2)
    ), 6) AS overlay_magnitude,
    ROUND(0.35 + ABS(SIN(seq.n * 0.3)) * 0.25 + ABS(((seq.n % 11) - 5) * 10.0) * 0.004, 6) AS residual_value,
    ROUND(0.086 + (((seq.n % 11) - 5) * 10.0) * 0.0011 + (FLOOR(seq.n / 11) - 5) * 0.0006, 6) AS focus_value,
    ROUND(42.30 + (((seq.n % 11) - 5) * 10.0) * 0.010 + (FLOOR(seq.n / 11) - 5) * 0.009, 6) AS dose_value,
    ROUND(0.990 - (ABS(((seq.n % 11) - 5) * 10.0) + ABS((FLOOR(seq.n / 11) - 5) * 10.0)) * 0.0009, 4) AS confidence,
    CASE WHEN ABS(SIN(seq.n * 0.5)) > 0.75 OR ABS((seq.n % 11) - 5) = 5 THEN 1 ELSE 0 END AS is_outlier,
    0
FROM measurement_run mr
JOIN seq
WHERE mr.run_no = 'MR-RICH-NOISE-PRE'
  AND NOT EXISTS (
      SELECT 1 FROM overlay_measurement_point x
      WHERE x.measurement_run_id = mr.id AND x.target_code = CONCAT('P', LPAD(seq.n + 1, 3, '0'))
  );

INSERT INTO simulation_result_summary (
    task_id, wafer_id, layer_id, measurement_run_id, mean_overlay, max_overlay, min_overlay, std_overlay, p95_overlay,
    pass_rate, pass_flag, warning_level, chart_snapshot_json, created_at, created_by
)
SELECT t.id, mr.wafer_id, mr.layer_id, mr.id, 5.38, 9.84, 1.48, 2.08, 8.67, 0.846, 0, 'MEDIUM',
       JSON_OBJECT('scenario', 'noise', 'density', 121), '2026-04-07 10:00:00', 0
FROM simulation_task t
JOIN measurement_run mr ON mr.run_no = 'MR-RICH-NOISE-PRE'
WHERE t.task_no = 'TASK-RICH-NOISE-001'
  AND NOT EXISTS (
      SELECT 1 FROM simulation_result_summary s WHERE s.task_id = t.id AND s.measurement_run_id = mr.id
  );

