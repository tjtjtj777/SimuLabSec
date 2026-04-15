CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_code VARCHAR(64) NOT NULL UNIQUE,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    email VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    preferred_language VARCHAR(16) NOT NULL,
    is_demo_account TINYINT NOT NULL DEFAULT 0,
    last_login_at DATETIME NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL UNIQUE,
    role_name VARCHAR(128) NOT NULL,
    description VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_role_user_role (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS demo_dataset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dataset_code VARCHAR(64) NOT NULL UNIQUE,
    dataset_name VARCHAR(128) NOT NULL,
    scenario_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    description VARCHAR(255) NULL,
    tags_json JSON NULL,
    seed_version VARCHAR(32) NOT NULL,
    loaded_at DATETIME NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS fab_layer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    layer_code VARCHAR(64) NOT NULL UNIQUE,
    layer_name VARCHAR(128) NOT NULL,
    layer_type VARCHAR(32) NOT NULL,
    sequence_no INT NOT NULL,
    description VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS fab_lot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lot_no VARCHAR(64) NOT NULL UNIQUE,
    product_id BIGINT NULL,
    lot_status VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    priority_level VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    wafer_count INT NOT NULL DEFAULT 0,
    collected_at DATETIME NULL,
    remark VARCHAR(255) NULL,
    dataset_id BIGINT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS fab_wafer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lot_id BIGINT NOT NULL,
    wafer_no VARCHAR(32) NOT NULL,
    wafer_status VARCHAR(32) NOT NULL,
    slot_no INT NULL,
    diameter_mm DECIMAL(8,2) NOT NULL DEFAULT 300.00,
    notch_direction VARCHAR(16) NULL,
    summary_tags_json JSON NULL,
    dataset_id BIGINT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_fab_wafer_lot_wafer_no (lot_id, wafer_no)
);

CREATE TABLE IF NOT EXISTS recipe (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_code VARCHAR(64) NOT NULL UNIQUE,
    recipe_name VARCHAR(128) NOT NULL,
    recipe_type VARCHAR(32) NOT NULL,
    product_id BIGINT NULL,
    layer_id BIGINT NULL,
    owner_user_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    description VARCHAR(255) NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS recipe_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    version_no INT NOT NULL,
    version_label VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    parent_version_id BIGINT NULL,
    parameter_schema_json JSON NULL,
    params_json JSON NULL,
    change_summary VARCHAR(255) NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sub_recipe (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sub_recipe_code VARCHAR(64) NOT NULL UNIQUE,
    recipe_version_id BIGINT NOT NULL,
    source_task_id BIGINT NULL,
    lot_id BIGINT NULL,
    wafer_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    generation_type VARCHAR(32) NOT NULL,
    param_delta_json JSON NULL,
    param_set_json JSON NULL,
    export_format VARCHAR(32) NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS simulation_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(64) NOT NULL UNIQUE,
    task_name VARCHAR(128) NOT NULL,
    lot_id BIGINT NULL,
    layer_id BIGINT NULL,
    recipe_version_id BIGINT NULL,
    scenario_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority_level VARCHAR(16) NULL,
    idempotency_key VARCHAR(128) NULL,
    input_snapshot_json JSON NULL,
    execution_context_json JSON NULL,
    result_summary_json JSON NULL,
    error_message VARCHAR(500) NULL,
    requested_by BIGINT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS measurement_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    run_no VARCHAR(64) NOT NULL UNIQUE,
    lot_id BIGINT NULL,
    wafer_id BIGINT NULL,
    layer_id BIGINT NULL,
    measurement_type VARCHAR(32) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    tool_name VARCHAR(128) NULL,
    sampling_count INT NOT NULL DEFAULT 0,
    import_file_id BIGINT NULL,
    measurement_context_json JSON NULL,
    summary_json JSON NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS overlay_measurement_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    measurement_run_id BIGINT NOT NULL,
    wafer_id BIGINT NULL,
    layer_id BIGINT NULL,
    mark_id BIGINT NULL,
    target_code VARCHAR(64) NOT NULL,
    x_coord DECIMAL(12,4) NOT NULL,
    y_coord DECIMAL(12,4) NOT NULL,
    overlay_x DECIMAL(12,6) NULL,
    overlay_y DECIMAL(12,6) NULL,
    overlay_magnitude DECIMAL(12,6) NULL,
    residual_value DECIMAL(12,6) NULL,
    focus_value DECIMAL(12,6) NULL,
    dose_value DECIMAL(12,6) NULL,
    confidence DECIMAL(6,4) NULL,
    is_outlier TINYINT NOT NULL DEFAULT 0,
    extra_metrics_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS simulation_result_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    wafer_id BIGINT NULL,
    layer_id BIGINT NULL,
    measurement_run_id BIGINT NULL,
    mean_overlay DECIMAL(12,6) NULL,
    max_overlay DECIMAL(12,6) NULL,
    min_overlay DECIMAL(12,6) NULL,
    std_overlay DECIMAL(12,6) NULL,
    p95_overlay DECIMAL(12,6) NULL,
    pass_rate DECIMAL(6,4) NULL,
    pass_flag TINYINT NOT NULL DEFAULT 0,
    warning_level VARCHAR(16) NULL,
    chart_snapshot_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS import_file_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_no VARCHAR(64) NOT NULL UNIQUE,
    dataset_id BIGINT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    biz_type VARCHAR(32) NOT NULL,
    storage_path VARCHAR(255) NULL,
    file_size BIGINT NULL,
    checksum VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    validation_summary_json JSON NULL,
    error_message VARCHAR(500) NULL,
    uploaded_by BIGINT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0
);
