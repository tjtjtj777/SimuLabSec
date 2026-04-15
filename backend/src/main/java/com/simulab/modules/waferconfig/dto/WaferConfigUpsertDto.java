package com.simulab.modules.waferconfig.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class WaferConfigUpsertDto {

    // configName / lotNo / waferNo / layerId 这四项决定了“这套分析配置是给谁用的”。
    private String configName;
    private String description;
    private String lotNo;
    private String waferNo;
    private Long layerId;
    private String measurementType;
    private String stage;
    // scannerCorrectionGain 是当前规则模型里最敏感的主参数，会显著影响热图空间场强度。
    private BigDecimal scannerCorrectionGain;
    private BigDecimal overlayBaseNm;
    private BigDecimal edgeGradient;
    private BigDecimal localHotspotStrength;
    private BigDecimal noiseLevel;
    // gridStep 决定点位密度；值越小，生成点数越多，计算与渲染成本也越高。
    private BigDecimal gridStep;
    private BigDecimal outlierThreshold;
}
