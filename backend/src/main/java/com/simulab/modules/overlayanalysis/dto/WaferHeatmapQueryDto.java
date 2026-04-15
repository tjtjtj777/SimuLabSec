package com.simulab.modules.overlayanalysis.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WaferHeatmapQueryDto {

    @Positive(message = "waferId 必须为正整数")
    private Long waferId;
    @Positive(message = "layerId 必须为正整数")
    private Long layerId;
    @Positive(message = "measurementRunId 必须为正整数")
    private Long measurementRunId;
    @Size(max = 32, message = "metricCode 长度不能超过 32")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]*$", message = "metricCode 格式不合法")
    private String metricCode;
}
