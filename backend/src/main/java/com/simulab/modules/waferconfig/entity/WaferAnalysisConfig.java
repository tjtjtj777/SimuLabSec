package com.simulab.modules.waferconfig.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.simulab.common.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wafer_analysis_config")
public class WaferAnalysisConfig extends BaseEntity {

    // 稳定业务编码，便于页面展示和接口回显。
    private String configNo;
    private String configName;
    private String description;
    // 当前阶段配置表用 lotNo / waferNo 作为业务锚点，而不是直接持有 lotId / waferId。
    private String lotNo;
    private String waferNo;
    private Long layerId;
    private String measurementType;
    private String stage;
    // 以下参数共同控制规则模型如何生成 overlay 空间分布。
    private BigDecimal scannerCorrectionGain;
    private BigDecimal overlayBaseNm;
    private BigDecimal edgeGradient;
    private BigDecimal localHotspotStrength;
    private BigDecimal noiseLevel;
    private BigDecimal gridStep;
    private BigDecimal outlierThreshold;
    private String status;
    // 最近一次基于这套配置生成的结果，便于页面回跳和历史追踪。
    private Long lastMeasurementRunId;
    private Long lastTaskId;
    private String configSummaryJson;
}
