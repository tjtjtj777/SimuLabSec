SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'overlay_measurement_point'
      AND index_name = 'idx_omp_run_wafer_layer_coord'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_omp_run_wafer_layer_coord ON overlay_measurement_point (measurement_run_id, wafer_id, layer_id, x_coord, y_coord)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'measurement_run'
      AND index_name = 'idx_measurement_run_scope'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_measurement_run_scope ON measurement_run (measurement_type, status, lot_id, wafer_id, layer_id, stage, created_by)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'simulation_result_summary'
      AND index_name = 'idx_sim_result_summary_query'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_sim_result_summary_query ON simulation_result_summary (task_id, wafer_id, layer_id, created_at)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'simulation_task'
      AND index_name = 'idx_sim_task_owner_deleted'
);
SET @idx_sql := IF(
    @idx_exists = 0,
    'CREATE INDEX idx_sim_task_owner_deleted ON simulation_task (created_by, deleted)',
    'SELECT 1'
);
PREPARE stmt FROM @idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
