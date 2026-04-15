CREATE TABLE IF NOT EXISTS wafer_analysis_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    config_no VARCHAR(64) NOT NULL,
    config_name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NULL,
    lot_no VARCHAR(64) NOT NULL,
    wafer_no VARCHAR(32) NOT NULL,
    layer_id BIGINT NOT NULL,
    measurement_type VARCHAR(32) NOT NULL DEFAULT 'OVERLAY',
    stage VARCHAR(32) NOT NULL DEFAULT 'PRE_ETCH',
    scanner_correction_gain DECIMAL(8,4) NOT NULL DEFAULT 1.0000,
    overlay_base_nm DECIMAL(10,4) NOT NULL DEFAULT 3.2000,
    edge_gradient DECIMAL(10,4) NOT NULL DEFAULT 1.3000,
    local_hotspot_strength DECIMAL(10,4) NOT NULL DEFAULT 1.0000,
    noise_level DECIMAL(10,4) NOT NULL DEFAULT 0.1500,
    grid_step DECIMAL(10,4) NOT NULL DEFAULT 0.5000,
    outlier_threshold DECIMAL(10,4) NOT NULL DEFAULT 8.0000,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_measurement_run_id BIGINT NULL,
    last_task_id BIGINT NULL,
    config_summary_json JSON NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wafer_analysis_config_no (config_no),
    KEY idx_wafer_analysis_config_owner (created_by, deleted),
    KEY idx_wafer_analysis_config_lot (lot_no, wafer_no, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO wafer_analysis_config (
    config_no, config_name, description, lot_no, wafer_no, layer_id, measurement_type, stage,
    scanner_correction_gain, overlay_base_nm, edge_gradient, local_hotspot_strength, noise_level,
    grid_step, outlier_threshold, status, created_by, updated_by
)
SELECT 'CFG-DEMO-STABLE', 'Demo Stable Baseline', 'Stable demo config for wafer analysis', 'LOT-STABLE-001', 'WS01', l.id,
       'OVERLAY', 'PRE_ETCH', 1.0000, 3.2000, 1.2000, 0.8000, 0.1200, 0.5000, 8.0000, 'ACTIVE', 0, 0
FROM fab_layer l
WHERE l.layer_code = 'LAYER-M1'
  AND NOT EXISTS (SELECT 1 FROM wafer_analysis_config WHERE config_no = 'CFG-DEMO-STABLE');

INSERT INTO wafer_analysis_config (
    config_no, config_name, description, lot_no, wafer_no, layer_id, measurement_type, stage,
    scanner_correction_gain, overlay_base_nm, edge_gradient, local_hotspot_strength, noise_level,
    grid_step, outlier_threshold, status, created_by, updated_by
)
SELECT 'CFG-DEMO-EDGE', 'Demo Edge Sensitive', 'Edge-sensitive demo config for wafer analysis', 'LOT-OUTLIER-001', 'WO01', l.id,
       'OVERLAY', 'PRE_ETCH', 1.1500, 3.6000, 1.9000, 1.6000, 0.1800, 0.5000, 8.5000, 'ACTIVE', 0, 0
FROM fab_layer l
WHERE l.layer_code = 'LAYER-VIA1'
  AND NOT EXISTS (SELECT 1 FROM wafer_analysis_config WHERE config_no = 'CFG-DEMO-EDGE');

INSERT INTO wafer_analysis_config (
    config_no, config_name, description, lot_no, wafer_no, layer_id, measurement_type, stage,
    scanner_correction_gain, overlay_base_nm, edge_gradient, local_hotspot_strength, noise_level,
    grid_step, outlier_threshold, status, created_by, updated_by
)
SELECT 'CFG-DEMO-DRIFT', 'Demo Drift Compare', 'Drift-sensitive demo config for wafer analysis', 'LOT-DRIFT-001', 'WD01', l.id,
       'OVERLAY', 'POST_ETCH', 0.9000, 2.9000, 1.7000, 1.1000, 0.2200, 0.5000, 7.8000, 'ACTIVE', 0, 0
FROM fab_layer l
WHERE l.layer_code = 'LAYER-M2'
  AND NOT EXISTS (SELECT 1 FROM wafer_analysis_config WHERE config_no = 'CFG-DEMO-DRIFT');
