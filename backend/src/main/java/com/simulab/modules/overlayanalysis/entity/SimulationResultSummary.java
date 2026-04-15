package com.simulab.modules.overlayanalysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("simulation_result_summary")
public class SimulationResultSummary {

    private Long id;
    private Long taskId;
    private Long waferId;
    private Long layerId;
    private Long measurementRunId;
    private BigDecimal meanOverlay;
    private BigDecimal maxOverlay;
    private BigDecimal minOverlay;
    private BigDecimal stdOverlay;
    private BigDecimal p95Overlay;
    private BigDecimal passRate;
    private Integer passFlag;
    private String warningLevel;
    private String chartSnapshotJson;
    private LocalDateTime createdAt;
    private Long createdBy;
}
