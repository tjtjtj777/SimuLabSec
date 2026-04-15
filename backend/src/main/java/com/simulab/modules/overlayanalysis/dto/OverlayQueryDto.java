package com.simulab.modules.overlayanalysis.dto;

import lombok.Data;

@Data
public class OverlayQueryDto {

    private Long waferId;
    private Long layerId;
    private Long measurementRunId;
    private String metricCode;
}
