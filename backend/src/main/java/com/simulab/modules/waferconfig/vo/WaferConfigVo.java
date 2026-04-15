package com.simulab.modules.waferconfig.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class WaferConfigVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String configNo;
    private String configName;
    private String description;
    private String lotNo;
    private String waferNo;
    private Long layerId;
    private String measurementType;
    private String stage;
    private BigDecimal scannerCorrectionGain;
    private BigDecimal overlayBaseNm;
    private BigDecimal edgeGradient;
    private BigDecimal localHotspotStrength;
    private BigDecimal noiseLevel;
    private BigDecimal gridStep;
    private BigDecimal outlierThreshold;
    // dataScope / editable / deletable 是前端直接消费的权限视图字段。
    private String dataScope;
    private Integer editable;
    private Integer deletable;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long lastMeasurementRunId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long lastTaskId;
    private LocalDateTime updatedAt;
    // 每个配置会带一组可直接展示给用户的校验规则，减少前端自己硬编码解释文案。
    private List<WaferConfigValidationRuleVo> validationRules;
}
