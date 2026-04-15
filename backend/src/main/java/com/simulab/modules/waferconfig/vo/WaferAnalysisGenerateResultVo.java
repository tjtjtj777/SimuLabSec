package com.simulab.modules.waferconfig.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class WaferAnalysisGenerateResultVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long configId;
    @JsonSerialize(using = ToStringSerializer.class)
    // task 是“业务任务记录”，用于任务列表和历史追踪。
    private Long taskId;
    private String taskNo;
    @JsonSerialize(using = ToStringSerializer.class)
    // measurementRun 是“数据头记录”，用于挂接 heatmap/scatter/histogram 的点位数据。
    private Long measurementRunId;
    private String measurementRunNo;
    private Long lotId;
    private Long waferId;
    private Long layerId;
    // 本次实际生成的点位数量，通常和 gridStep 直接相关。
    private Integer generatedPoints;
    private BigDecimal meanOverlay;
    private BigDecimal p95Overlay;
    private BigDecimal maxOverlay;
    private BigDecimal stdOverlay;
    private BigDecimal passRate;
    private String overallQuality;
    private String overlayStability;
    private String edgeRisk;
    private String outlierDensity;
    private String parameterSensitivity;
    private String recommendedAction;
    // 面向前端直接展示的通俗总结，已根据 locale 生成中英文。
    private String summaryText;
    private Boolean reuseHit;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reusedRunId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reusedTaskId;
    private List<String> validationErrors;
    private Long elapsedMs;
}
