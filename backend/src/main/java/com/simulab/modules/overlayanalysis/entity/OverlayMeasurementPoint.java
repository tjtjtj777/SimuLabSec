package com.simulab.modules.overlayanalysis.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("overlay_measurement_point")
public class OverlayMeasurementPoint {

    private Long id;
    private Long measurementRunId;
    private Long waferId;
    private Long layerId;
    private Long markId;
    private String targetCode;
    private BigDecimal xCoord;
    private BigDecimal yCoord;
    private BigDecimal overlayX;
    private BigDecimal overlayY;
    private BigDecimal overlayMagnitude;
    private BigDecimal residualValue;
    private BigDecimal focusValue;
    private BigDecimal doseValue;
    private BigDecimal confidence;
    private Integer isOutlier;
    private String extraMetricsJson;
    private LocalDateTime createdAt;
    private Long createdBy;
}
